# QR-code Device Owner Provisioning — Contract

Covers the fleet-deployment enrollment path: scanning a QR code during a
fresh device's Setup Wizard grants this app Device Owner and auto-enrolls it
with the backend, in one scan — no manual token entry via `EnrollmentScreen`.

This is standard Android `ACTION_PROVISION_MANAGED_DEVICE` QR provisioning.
It is unrelated to AMAPI (the separate, GMS-only, non-functional-on-this-app's-
target-devices path already flagged in the codebase map).

## Division of responsibility

- **Backend** generates and hosts the QR code (JSON payload below), hosts the
  release APK for Setup Wizard to download, and computes the signing
  checksum. None of that lives in this repo.
- **App** (`enterprise/admin/receiver/MyDeviceAdminReceiver.kt`,
  `sync/ProvisioningEnrollWorker.kt`) only receives the
  `onProfileProvisioningComplete()` callback Android fires once Setup Wizard
  has finished, reads the enrollment token out of the admin extras bundle,
  and enrolls via the same `EmmRepository.enroll()` call the manual
  token-entry path already uses.

`onProfileProvisioningComplete()` fires *only* for genuine Setup-Wizard-driven
provisioning (QR/NFC/zero-touch) — it is never invoked by `adb shell dpm
set-device-owner`, which is what's been used for manual testing throughout
this project so far. The two paths don't interfere with each other.

## Confirmed contract (cross-session, 2026-08-26)

Admin extras bundle key for the enrollment token: **`enrollmentToken`** — the
same string already used as the placeholder QR JSON's field name and as
`EnrollRequestDto.enrollmentToken` / the real `/api/dpc/enroll` request body.
One word, no translation table to keep in sync between the two sides.

Expected QR JSON shape:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME":
    "com.floydwiz.googlemdm/.enterprise.admin.receiver.MyDeviceAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION":
    "<https URL to the release APK>",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM":
    "<base64 SHA-256>",
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "enrollmentToken": "<tokenValue>"
  }
}
```

### Two correctness pitfalls, both easy to get wrong silently

1. **Component name must include the full package path.**
   `com.floydwiz.googlemdm/.MyDeviceAdminReceiver` is **wrong** —
   `MyDeviceAdminReceiver` lives in
   `com.floydwiz.googlemdm.enterprise.admin.receiver`, not the app's root
   package (see `AndroidManifest.xml`'s `<receiver android:name=".enterprise.admin.receiver.MyDeviceAdminReceiver">`
   and the file's own `package` declaration). A dot-prefixed class name in
   `ComponentName` resolves relative to the *application* package, not
   wherever the class actually is — get this wrong and Setup Wizard fails to
   resolve the admin before our code ever runs.

2. **The signature checksum hashes the signing certificate, not the APK
   file.** `sha256sum app-release.apk` is the wrong command. The checksum
   must be the base64 SHA-256 of the certificate used to sign the APK
   (extract via `keytool -exportcert` / `apksigner`), matching what
   `DevicePolicyManager` verifies against post-install.

## App-side flow

1. Setup Wizard downloads + installs the APK, sets Device Owner, then
   broadcasts completion to `MyDeviceAdminReceiver`.
2. `onProfileProvisioningComplete()` reads
   `DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE`, extracts
   `enrollmentToken`, and enqueues `ProvisioningEnrollWorker` (a one-time
   `WorkManager` job — a `BroadcastReceiver` callback can't safely make a
   blocking network call itself).
3. `ProvisioningEnrollWorker` calls `EmmRepository.enroll(token)` — the exact
   same call `EmmViewModel.enroll()` makes for manual entry — then
   `CheckInScheduler.ensureScheduled(...)` on success. `Result.retry()` on
   failure rides out the case where network isn't fully up the instant
   provisioning completes, via WorkManager's linear backoff.
4. If the extras bundle is missing or has no `enrollmentToken` (e.g. Device
   Owner was granted through some other provisioning trigger), the app logs
   a warning and does nothing further — the device is still Device Owner,
   and a user can complete enrollment manually from `EnrollmentScreen`'s
   "EMM Backend" card, same as today.

## Testing

Blocked until the backend's APK hosting + checksum generation is ready (in
progress as of this writing). Once available:

1. Factory-reset (or a fresh emulator) a test device.
2. During Setup Wizard, trigger the QR scanner (typically 6 taps on the
   Welcome screen) and scan the backend-generated QR.
3. Confirm Device Owner: `adb shell dumpsys device_policy | grep -A3 "Device Owner"`.
4. Confirm enrollment: check the backend dashboard for the device, and/or
   `adb logcat` for `"QR-provisioning auto-enroll succeeded"`.

Same verification technique already used for the kiosk and command-execution
features — direct `adb`/`dumpsys` checks against a real device rather than
relying on logs alone.
