# Full Testing Checklist

A working QA checklist covering everything built across `frontend`, `Amapi_Work`, and
`android-device-policy`. Each section notes which branch it applies to. Edge cases are
inline with the feature they belong to, not bucketed separately — most are real gotchas
hit during this session, not hypotheticals.

## 0. Setup / prerequisites

- [*] `adb pair`/`adb connect` works with a fresh pairing code (codes are short-lived — a
      "protocol fault" error on `adb pair` usually means the code expired, not a real failure).
- [*] `adb devices -l` shows exactly one entry per physical device — the same device often
      shows up twice (direct IP:port + auto-reconnected mDNS `_adb-tls-connect._tcp` entry);
      disconnect the duplicate before running device-specific commands, or `-s <serial>` calls
      can silently target the wrong entry / fail with "more than one device/emulator".
- [*] `dpm set-device-owner` succeeds on a factory-fresh device (or one with zero accounts added).
  - [*] **Edge case**: fails with "Invalid component ... for device owner" if the APK isn't
        installed yet at all — install first, then set device owner.
  - [*] **Edge case**: fails with "already some accounts on the device" if any Google/other
        account was added — remove all accounts first (Settings → Accounts), or factory reset.
- [ ] `adb reverse tcp:8080 tcp:8080` set up before testing anything that hits the local dev
      backend (`EMM_BASE_URL`). Re-run after every device reboot (reverse mappings don't survive).
- [ ] **Edge case**: `force-stop`/direct `kill <pid>` do **not** work on this app once it's
      Device Owner on some ROMs/builds — the process silently survives. If a test needs a truly
      fresh process (e.g. re-reading a preference at construction time), reboot the device
      instead of relying on force-stop.

## 1. Device Owner & Admin lifecycle (`Amapi_Work`, `frontend`, `android-device-policy`)

- [*] Fresh install, no device owner → Overview/Dashboard shows "Device Admin: Inactive",
      "Device Owner: Not Owner".
- [*] Tap "Activate Device Admin" → system dialog appears → accept → status flips to Active.
- [*] `dpm set-device-owner` via adb → app-side status correctly reflects Owner without
      needing the manual admin-activation button.
- [*] "Refresh" button re-reads real device state (don't rely on stale cached UI state).

## 2. Enterprise Policies — Camera / Screen Capture / USB / Safe Boot / Factory Reset / Add User / Outgoing Calls / SMS

- [*] Each toggle actually changes real device behavior, not just the UI switch:
  - [*] Camera disabled → camera app/camera permission genuinely blocked.
  - [*] Screen Capture disabled → screenshot attempt fails/blocked.
  - [*] USB File Transfer disabled → USB file transfer mode unavailable.
  - [] Safe Boot disabled → can't boot into safe mode via hardware buttons.
  - [*] Factory Reset disabled → Settings' factory-reset option is greyed out/blocked.
  - [*] Add User disabled → can't add a new user profile.
  - [] Outgoing Calls / SMS disabled → dialer/messaging genuinely blocked (where applicable to the device type).
- [*] Toggle off → toggle back on works (not just one-directional).
- [*] Policy state survives app restart and device reboot (persisted via `DevicePolicyManager`, not just in-memory).
- [*] Toggling requires active Device Admin — verify each setter's guard (`isAdminActive()`
      check) actually blocks the change when admin isn't active, rather than silently no-op'ing
      in a way that looks successful in the UI.

## 3. Network Policy (WiFi)

- [*] "Disable Wi-Fi Configuration" toggle prevents the user from changing WiFi settings
      manually (Settings → WiFi should be locked down), independent of whether WiFi itself is on/off.
- [*] WiFi enabled/disabled state displayed matches actual device WiFi radio state.
- [*] (android-device-policy) `connectToWifiNetwork()` — test both `OPEN` and `*_PSK` security
      types; confirm it's a no-op (not a crash) for unrecognized security types.

## 4. Kiosk Mode — local toggle, backend-driven, and the new `KioskScreen`

- [*] **Local toggle** (Policies tab / Dashboard switch): enabling calls `setLockTaskPackages`,
      immediately engages `startLockTask()` — home button, recents, notification shade should
      all be blocked while active.
- [*] Disabling via the same toggle correctly calls `stopLockTask()` and releases the device.
- [ ] **New `KioskScreen` gating** (`frontend` + ported to `Amapi_Work`): the moment kiosk mode
      is enabled — from *any* source — the entire app UI swaps to `KioskScreen` (dummy app
      grid), not the normal MDM screen. Verify this happens:
  - [ ] On enabling from the local Policies-tab toggle, live, while already on that screen.
  - [ ] On app launch, if kiosk was already enabled from a previous session.
  - [ ] From a **backend-driven** policy check-in (see §5) while the app is already in the foreground.
- [ ] "Exit Kiosk Mode (Admin)" button on `KioskScreen` correctly disables kiosk and reactively
      swaps back to the normal UI (Splash on `frontend`, Enrollment on `Amapi_Work`) — no relaunch needed.
- [ ] **Edge case**: `LazyVerticalGrid` inside a `Column` needs `.weight(1f)` or it greedily
      fills all space and pushes/collapses anything below it (the Exit button) off-screen —
      regression-check this specifically if the dummy grid's item count or layout ever changes.
- [ ] **Edge case**: on this device family, taps very close to the bottom of the screen (within
      roughly the last ~55px) can land on the system nav-bar's overlapping strip instead of app
      content underneath. If a bottom-of-screen tap "does nothing," retest a bit higher before
      assuming a code bug.
- [ ] **Backend-driven kiosk policy** with `allowedPackageNames` empty → correctly refused
      ("refusing to lock the device to nothing"), does **not** apply, logs a clear warning —
      verify this guard still fires and doesn't silently lock to an empty allow-list.
- [ ] Backend-driven kiosk policy with a real `allowedPackageNames` (must include
      `com.floydwiz.googlemdm` itself for this app to actually enter lock task) → applies
      correctly on next check-in, live-engages if foregrounded, or on next resume if backgrounded.

## 5. Custom EMM Backend — Enrollment, Check-In, Commands (`frontend`, `Amapi_Work`)

- [ ] Enroll via manual token entry → success updates "Enrolled: Yes" + real Device ID shown.
- [ ] Enroll via QR scan → same result; camera permission prompt appears on first use.
- [ ] **Edge case**: tapping Enroll while already enrolled is a no-op (button should be disabled
      once `isEnrolled` is true — verify it actually greys out, not just logically blocked).
- [ ] Wrong/expired token → clear error message, no crash.
- [ ] "Reset / Unenroll" clears local state only — verify the UI correctly reflects "Not
      Enrolled" afterward, and that this does **not** claim to have unenrolled server-side.
- [ ] "Check In Now" — verify last-check-in timestamp and polling interval update.
  - [ ] **Edge case**: requested interval below WorkManager's 15-minute periodic-work floor
        gets clamped, with a warning logged — verify no crash, and the *actual* scheduled
        interval is the floor, not the requested value.
- [ ] Check-in while not enrolled → clear "not enrolled" error, no network call attempted, no crash.
- [ ] Check-in returning HTTP 401 → local credentials cleared, UI flips to "Not Enrolled" with a
      clear message, no crash.
- [ ] Commands delivered via check-in (`LOCK`, `WIPE`, `REBOOT`, `RESET_PASSWORD`,
      `CLEAR_APP_DATA`, `REQUEST_DEVICE_INFO`) each execute and ack correctly:
  - [ ] Non-destructive commands (`RESET_PASSWORD`, `CLEAR_APP_DATA`, `REQUEST_DEVICE_INFO`)
        ack immediately after executing.
  - [ ] Destructive commands (`WIPE`, `REBOOT`) ack **before** executing (device may not survive
        to ack afterward) — verify via backend logs that the ack lands even if you can't observe
        the device post-wipe/reboot.
  - [ ] `isReadyForDestructiveCommand()` (i.e. not Device Owner) correctly blocks a destructive
        command with a `FAILED` ack, rather than attempting it.
- [ ] Debug-only "Install Dev Backend CA Cert" button installs successfully and unblocks TLS
      trust to the local dev backend (retry Enroll afterward if it was previously failing on
      cert trust).

## 6. AMAPI Environment Prep (`Amapi_Work` — its own EnrollmentScreen section)

- [ ] "Check Environment" reflects real Android Device Policy install/version state.
- [ ] "Prepare AMAPI Environment" triggers install/consent flow for Android Device Policy if
      not already installed — verify each real failure path surfaces distinctly rather than one
      generic error: not installed, install consent declined/dismissed, needs Play Store update,
      unrecoverable install error.

## 7. Android Device Policy / AMAPI companion mode (`android-device-policy` only)

- [ ] Reached via "Android Device Policy Mode" button on EnrollmentScreen — verify this doesn't
      alter anything on the existing custom-DPC Dashboard/Enrollment flow (dual-support: nothing
      shared should regress).
- [ ] **GMS Account Setup**: enter a real (non-one-time) enrollment token → Start Account Setup.
  - [ ] **Edge case**: a **one-time** token can get consumed on an intermediate round-trip
        inside `AccountSetupClient`'s own pre-authentication step, then legitimately come back
        `FAILURE_REASON_ENROLLMENT_TOKEN_INVALID` / HTTP 403 on a *later* step — always mint
        non-one-time tokens for repeated testing.
  - [ ] A genuinely invalid/garbage token → clear `ACCOUNT_SETUP_FAILURE_REASON_UNKNOWN` surfaced
        in the UI; check `clouddpc`'s own system logcat (not just app logs) for the real
        underlying reason if this needs deeper debugging (app-side only sees a generic bucket).
  - [ ] A valid token → progresses through `PREPARE_AUTHENTICATION` → `LAFORGE_PROVISIONING` →
        `LAFORGE_REGISTER_DEVICE` → `SEND_LAFORGE_INFO`, then surfaces real `userId`/`deviceId`
        once it reaches `ADDED_ACCOUNT` state — **or** fails at "add work account" with
        `BAD_AUTHENTICATION`/`ApiException: 13` if the enterprise hasn't cleared Google's
        managed-Play-account certification wall (expected/external until that's resolved — not
        an app or backend bug; don't waste time re-debugging this exact signature).
- [ ] "Check Status" correctly picks up the async result from the push-delivered
      `AccountSetupListener` (the initial `startAccountSetup()` return value is often stale
      `IN_PROGRESS` — always re-check status rather than trusting the immediate return).
- [ ] **Migrate to Android Device Policy**: empty token → validation error, no network call.
      Real migration token → real result/error from `DpcMigrationClient` (needs the backend's
      migration-token endpoint fed real `userId`/`deviceId` from a completed account setup first).
- [ ] **Device (via AMAPI)**: "Load Device Info" on a device not yet granted an AMAPI role →
      expect a clean "Permission denied" surfaced in the UI, not a crash.
- [ ] **Regression check**: any screen that triggers a real AMAPI SDK RPC round-trip
      (`AccountSetupClient`, `LocalCommandClient`, `DeviceClient`, `DpcMigrationClient`) — verify
      no `NoSuchMethodError`/`FATAL EXCEPTION` on `SendChannel.close$default`. This was a real
      crash caused by `kotlinx-coroutines-core` resolving to 1.10.2 instead of the 1.11.0+ AMAPI
      1.8.2 actually needs; if it resurfaces, check `app/build.gradle.kts`'s coroutines version
      first before assuming new code broke something.

## 8. UI / Navigation (`frontend` only)

- [ ] Splash screen shows on every fresh launch (by design — not a "remember last mode" bug).
- [ ] "Device Console" → bottom-nav (Overview / Network / Policies) — switching tabs preserves
      each tab's scroll/state (`saveState`/`restoreState` on nav).
- [ ] System Back button from Overview/Network/Policies or EMM Backend returns to Splash
      (no `popUpTo` removes Splash from the back stack, so this should always work).
- [ ] Policies tab's bottom-nav badge shows the live count of currently-enabled policies —
      verify it updates immediately when a policy is toggled, not just on next screen load.
- [ ] Dark theme renders correctly (no default-Material-purple leaking through anywhere) and the
      app icon is the gradient "M" mark, not the default Android Studio robot.
- [ ] Kiosk-mode gating (§4) takes priority over the Splash/Console/EMM-Backend navigation
      entirely — confirm you truly cannot navigate to any of those while kiosk mode is on.

## 9. Cross-cutting regressions

- [ ] Toggling any policy or kiosk mode via the backend check-in path vs. the local UI path
      produce the *same* end state (persisted pref, DPM-level policy, and in-memory `StateFlow`
      all agree) — test each policy from both directions at least once.
- [ ] Kill the app process (or reboot) with kiosk mode active → relaunch → still correctly
      shows `KioskScreen` (persisted state read correctly on cold start, not just live updates).
- [ ] Two devices connected via adb simultaneously — always double check which serial a command
      actually targeted; a command issued without `-s <serial>` when 2+ devices are attached
      either fails outright or silently hits the wrong device.
- [ ] After any `git merge`/cherry-pick between branches with real divergence (e.g. porting a
      feature from `Amapi_Work`/`android-device-policy` into `frontend` or vice versa), diff the
      result against a plain `git status --short` before committing — a full merge across these
      branches drags back in files one branch deliberately deleted (old `DashboardScreen.kt`,
      `EnrollmentScreen.kt`, etc.); prefer manually porting just the changed files instead.
