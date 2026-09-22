# Work Summary — App Restrictions, Wi-Fi, and Backend Connectivity

Branch: `frontend`. Covers the work completed since the last status doc
(`docs/TESTING_CHECKLIST.md`), bundled in commit `d1dc31f` plus the
localhost/adb-reverse revert on top of it (currently uncommitted).

## 1. Wi-Fi push actually joins the network (fixed)

**Problem:** `NetworkManager.connectToWifiNetwork()` only registered the
network config with Android (`addNetworkPrivileged()` on API 30+, legacy
`addNetwork()` below that) — it never told Android to actually connect to
it. Confirmed via `cmd wifi list-networks` showing the network added but
never joined.

**Fix:** after a successful add, calls `wifiManager.enableNetwork(networkId,
true)` to force the connection. Verified the real API via `javap` against
the SDK 36 `android.jar` before writing it — `enableNetwork` remains fully
functional for Device Owner apps (unlike regular apps, where it's a no-op
from targetSdk 29+).

**File:** `enterprise/network/manager/NetworkManager.kt`

**Known side effect:** every check-in that includes a `wifiConfig` block
now forces a real Wi-Fi reassociation, even if already connected to that
exact network — briefly drops the device's IP/adb connection each time.
Not wrong, just worth knowing if a policy always carries a wifi config.

## 2. REQUIRED app silent install (built + device-tested)

Backend can now attach `apkUrl`/`apkSha256` to a `REQUIRED` appRestrictions
entry; the device downloads, verifies, and installs it with **no user
interaction**.

- **New: `enterprise/apprestrictions/manager/ApkInstaller.kt`** — downloads
  the APK, computes and checks SHA-256 before touching anything else, then
  commits a `PackageInstaller` session with
  `SessionParams.USER_ACTION_NOT_REQUIRED` (silent install — only works
  because this app is Device Owner).
- **`AppRestrictionsManager.apply()`** — for a `REQUIRED` package that
  isn't installed, calls `ApkInstaller` when the backend supplied a source;
  otherwise logs the same "no source" warning as before. `BLOCKED`/
  `AVAILABLE` behavior unchanged.
- **`ApiModels.kt`** — `AppRestrictionPayloadDto` gained optional
  `apkUrl`/`apkSha256` fields.
- `PolicyApplier`/`DevicePolicyApplier`/`FakePolicyApplier` updated to
  `suspend fun apply(...)` to allow the blocking download+install (safe —
  already runs under `Dispatchers.IO` from `EmmRepository.checkIn()`).

**Verified live:** built a throwaway test app, had the backend deliver it
via check-in, watched it get silently installed and confirmed via
`pm list packages` / `dumpsys package` (`installerPackageName=
com.floydwiz.googlemdm`).

**Known bad data found (not an app bug):** an existing `REQUIRED` entry for
Snapchat and later WhatsApp pointed `apkUrl` at a Play Store/third-party
listing **page** (HTML), not a real APK binary — checksum verification and
then `PackageInstaller`'s own parse check both correctly reject it
(`INSTALL_PARSE_FAILED_NOT_APK`). This silent-install path is designed for
internal/self-hosted APKs, not Play Store apps. Fixing it requires either
hosting a real APK binary (e.g. pulled via `adb pull` from a device that
has it installed through Play Store, noting split-APK apps aren't
supported by our single-file `PackageInstaller` session) or dropping
`apkUrl`/`apkSha256` and leaving the entry as a plain `REQUIRED` with no
source.

## 3. BLOCKED enforcement — sideloads now get auto-removed

Previously `BLOCKED` only called `setApplicationHidden(hidden=true)`, which
only works on an app that's already installed — it did nothing to stop a
fresh sideload/reinstall.

- **New: `enterprise/apprestrictions/BlockedPackagesStore.kt`** — persists
  the backend's current `BLOCKED` package list locally, so enforcement
  doesn't depend on the next check-in.
- **New: `enterprise/apprestrictions/receiver/BlockedPackageInstallReceiver.kt`**
  — reacts to `ACTION_PACKAGE_ADDED` for *any* package; if it's on the
  blocked list, silently uninstalls it.
- **New: `enterprise/apprestrictions/service/BlockedPackageEnforcementService.kt`**
  — a persistent foreground service (always-on notification: "Device
  managed by Google MDM"). Starts on enroll, on app process start (if
  already enrolled), and on boot (new
  `enterprise/apprestrictions/receiver/BootCompletedReceiver.kt`).
- **`DeviceAdminManager.silentlyUninstall()`** — uses
  `PackageInstaller.uninstall()`, same Device-Owner no-confirmation
  exemption as the install path.
- **`AppRestrictionsManager.syncBlockedPackages()`** — recomputes the
  blocked set from the full policy on every check-in, so a package removed
  from `BLOCKED` server-side gets un-blocked on-device too.

**Two real bugs found and fixed getting this working (both confirmed via
`dumpsys activity broadcasts`, not guessed):**

1. **Package-visibility filtering (API 30+)** silently dropped the
   broadcast for packages we hadn't declared visibility into. Fixed with
   `android.permission.QUERY_ALL_PACKAGES` (legitimate/Play-Store-permitted
   for this exact device-management use case).
2. **Android's background-execution limits** silently drop a
   *manifest-declared* implicit-broadcast receiver's delivery once the app
   has sat in the background a while — confirmed identically affecting
   Play Store's and GMS's own equivalent receivers, and a foreground
   service running does **not** exempt a manifest receiver from this. Fix:
   `BlockedPackageInstallReceiver` is no longer manifest-declared at all —
   it's constructed and registered **dynamically**
   (`Context.registerReceiver`) from inside
   `BlockedPackageEnforcementService` while its process is alive, which is
   not subject to that limit.

**Verified live:** backgrounded the app for 100+ seconds (well past any
grace window), sideloaded a test package while `BLOCKED`, watched it get
silently removed within ~1 second (`Silent uninstall of ...: status=0`).
Repeated successfully on two separate devices (BraveArk, Primebook).

## 4. Enrollment/check-in connectivity — localhost + adb reverse (current)

The app talks to the local dev EMM backend at `https://localhost:8080/`,
reached via `adb reverse tcp:8080 tcp:8080`. This tunnel does **not**
survive the device's adb connection resetting (Wi-Fi reassociation,
reboot, mDNS reconnect, etc.), which happens often — every time it drops,
check-in/enrollment fails with `Failed to connect to localhost/
127.0.0.1:8080` until `adb reverse` is re-run.

**A LAN-IP-based alternative was built, tested working end-to-end, and then
reverted back to localhost+adb-reverse per explicit request** (commit
`d1dc31f` has the LAN IP version; the revert on top is currently
uncommitted). Two pieces of that work were kept because they're
improvements regardless of which base URL is used:

- **Backend's dev TLS cert (`secrets/dev-keystore.p12`, backend repo)**
  was regenerated by the backend session with a proper `Subject
  Alternative Name` (previously `CN=localhost` only, no SAN — modern TLS,
  including Android's, rejects CN-only certs). The new cert's SAN covers
  `localhost`, `127.0.0.1`, and both LAN IPs seen this session. It's a
  superset, so it still works fine for the localhost+reverse setup.
  `app/src/debug/assets/emm_dev_cert.der` was updated to match.
- **`ApiClient.kt`/`NetworkModule.kt`** — removed the old
  `DevHostnameOverride` custom `HostnameVerifier`/`CertificatePinner`
  workaround entirely. It existed only because the old cert had no SAN;
  now that the cert has a real one, standard TLS verification works and
  the workaround (which was hardcoded to `localhost`/`10.0.2.2` and pinned
  to the *old* cert's hash) is gone.

**Current values:**
- `app/build.gradle.kts`: `EMM_BASE_URL = "https://localhost:8080/"`
- `network_security_config.xml`: trusted/cleartext-permitted domains are
  `localhost` and `10.0.2.2` only.

**If check-in/enrollment errors with a connection failure:** run
`adb reverse tcp:8080 tcp:8080` again for the current adb connection.

**If revisiting the LAN-IP approach later:** point `EMM_BASE_URL` at this
machine's current LAN IP, add that IP as a trusted domain in
`network_security_config.xml`, and — if the machine's IP has changed since
the cert was generated — ask the backend session to regenerate
`secrets/dev-keystore.p12` with the new IP added to the SAN list. This
machine's own LAN IP is DHCP-assigned and *not* guaranteed stable; a
router-side DHCP reservation for this machine would make that approach
fully "set and forget."

## 5. Investigated: on-device "PrimeStore" app for silent installs

`com.floydwiz.primestore` (a system/privileged app on this Floydwiz ROM)
has real silent-install machinery
(`receivers.SilentInstallReceiver` → `SilentInstallWorker`, extras
`extra_package`/`extra_apk_path`/etc., decompiled to confirm — not a
stub). **Not currently usable as a REQUIRED-install backend**, though:
the receiver requires
`android:permission="com.android.launcher3.permission.SILENT_INSTALL"`,
and that permission is not declared by any installed package on this
device (checked the full permissions dump and the launcher's own manifest
specifically) — an undeclared custom permission can't be granted to any
app, including ours even if requested, and Device Owner status doesn't
override this. No exported Activity/Service/AIDL alternative exists
either. Our `apkUrl` + SHA-256 + `PackageInstaller` approach (section 2)
remains the right one unless a different device/ROM build actually
declares that permission.

## 6. Device Owner / Device Admin

`com.floydwiz.googlemdm` is set as Device Owner (which also establishes it
as active Device Admin) via:
```
adb shell dpm set-device-owner com.floydwiz.googlemdm/.enterprise.admin.receiver.MyDeviceAdminReceiver
```
Requires: app installed, zero user accounts on the device, single user
(no leftover work profile). Currently set on BraveArk and Primebook test
devices.

## 7. Kiosk screen (carried over from earlier session work, in this commit)

Dedicated `presentation/screens/kiosk/KioskScreen.kt` (dummy app grid +
Exit button) shown instead of the normal MDM UI whenever kiosk mode is
active, gated via `AppNavigation.kt` / `KioskGateViewModel.kt` /
`KioskManager.kioskModeState`.

## 8. Docs

`docs/TESTING_CHECKLIST.md` — full manual QA checklist covering the app,
including edge cases, added this round.
