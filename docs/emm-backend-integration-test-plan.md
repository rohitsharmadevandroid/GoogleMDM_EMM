# EMM Backend Integration — Manual Test Procedure

Covers the first milestone: enrollment, secure credential storage, check-in,
and server-controlled polling against the local dev backend.

## Prerequisites

- Backend running locally on `https://localhost:8080` with its self-signed dev
  TLS certificate.
- A physical device or emulator with `adb` connected.
- The backend's dev CA/certificate available to install on the device.

## 1. Start the backend

Start the EMM backend so it's listening on `localhost:8080`.

## 2. Make the backend reachable from the device and trust its cert

```
adb reverse tcp:8080 tcp:8080
```

This lets the app use the literal `https://localhost:8080` base URL from the
device/emulator, matching both the DPC contract and the dev cert's hostname.

Install the backend's dev certificate as a user certificate on the device
(Settings → Security → Encryption & credentials → Install a certificate → CA
certificate). This is required because `app/src/debug/res/xml/network_security_config.xml`
only trusts `system` and `user`-installed CAs for `localhost`/`10.0.2.2` — it
does not trust-all. This config only ships in debug builds.

## 3. Create a NON_GMS enrollment token

Create an enrollment token via the backend for a `NON_GMS` device (however
the backend exposes that — admin API/console/CLI). Note the token value.

## 4. Install Google_MDM

```
./gradlew :app:installDebug
```

## 5. Enter the token and enroll

1. Open the app. On the Enrollment screen, scroll to the **EMM Backend →
   Enrollment** card.
2. Paste the enrollment token into the **Enrollment Token** field.
3. Tap **Enroll**.
4. Expected: "Enrolled: Yes" and a **Device ID** are displayed. If enrollment
   fails, the error card shows the failure reason (HTTP status or network
   error) — this is the "see enrollment success/failure" requirement.

## 6. Verify backend device is created

On the backend, confirm a device record now exists for the deviceId shown in
the app, associated with the enrollment token used.

## 7. Verify device status PROVISIONING → ACTIVE after check-in

1. In the app's **EMM Backend → Check-In** card, tap **Check In Now**.
2. Expected: **Last Successful Check-In** updates to the current time, and
   **Server Polling Interval** reflects the value the backend returned.
3. On the backend, confirm the device's status transitioned from
   `PROVISIONING` to `ACTIVE`.

## 8. Verify backend `lastSeenAt` updates

Trigger another manual check-in (or wait for the background poll) and confirm
`lastSeenAt` on the backend device record advances each time.

## 9. Verify the Android app receives the check-in response

Confirm in Logcat (filtered to the app, e.g. `adb logcat | grep floydwiz`)
that a log line like `Check-in succeeded: policyVersion=..., pendingCommands=...,
intervalSeconds=...` appears after each check-in, with **no `deviceApiKey`
value ever present in the log output** (verify by searching Logcat output for
the raw key value — it must not appear).

## Background polling

- After enrollment, a periodic `CheckInWorker` is scheduled via WorkManager
  under the unique work name `emm_check_in_work`.
- WorkManager enforces a 15-minute minimum periodic interval; if the backend
  requests a shorter interval, the app clamps to 15 minutes and logs a
  warning (`Server requested Xs check-in interval, ... clamping to the
  floor.`). Confirm this by setting `checkInIntervalSeconds` on the backend
  below 900 and checking Logcat for the warning.
- Verify scheduling state: `adb shell dumpsys jobscheduler | grep -A 5 floydwiz`
  (WorkManager periodic work is backed by JobScheduler on API 23+).

## Negative cases to spot-check

- **Missing credentials**: uninstall/reinstall the app (clearing storage) and
  tap **Check In Now** before enrolling — expect the "Device is not enrolled
  yet" error, and confirm via Logcat/backend that no HTTP request was sent.
- **401 handling**: revoke/rotate the device's credential on the backend (if
  supported) or manually corrupt it, then check in — expect "Credential
  rejected by backend (401)" in the UI, no crash.
