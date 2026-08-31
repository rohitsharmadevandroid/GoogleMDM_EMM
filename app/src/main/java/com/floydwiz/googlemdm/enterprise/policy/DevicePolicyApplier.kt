package com.floydwiz.googlemdm.enterprise.policy

import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.data.remote.AppRestrictionPayloadDto
import com.floydwiz.googlemdm.data.remote.CustomDpcPolicyPayloadDto
import com.floydwiz.googlemdm.data.remote.KioskModePayloadDto
import com.floydwiz.googlemdm.data.remote.PasswordPolicyPayloadDto
import com.floydwiz.googlemdm.data.remote.WifiConfigPayloadDto
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.apprestrictions.manager.AppRestrictionsManager
import com.floydwiz.googlemdm.enterprise.kiosk.manager.KioskManager
import com.floydwiz.googlemdm.enterprise.network.manager.NetworkManager
import com.floydwiz.googlemdm.enterprise.policy.handler.PolicyHandler
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps the backend's CustomDpcPolicyPayload fields onto the existing
 * PolicyHandler/DeviceAdminManager device-owner calls - the same ones the
 * Dashboard's manual policy toggles already use.
 */
@Singleton
class DevicePolicyApplier @Inject constructor(
    private val policyHandler: PolicyHandler,
    private val kioskManager: KioskManager,
    private val deviceAdminManager: DeviceAdminManager,
    private val networkManager: NetworkManager,
    private val appRestrictionsManager: AppRestrictionsManager
) : PolicyApplier {

    override fun apply(payload: CustomDpcPolicyPayloadDto) {
        val results = listOf(
            PolicyType.CAMERA to policyHandler.setPolicy(PolicyType.CAMERA, payload.cameraDisabled),
            PolicyType.FACTORY_RESET to policyHandler.setPolicy(PolicyType.FACTORY_RESET, payload.factoryResetDisabled),
            PolicyType.SCREEN_CAPTURE to policyHandler.setPolicy(PolicyType.SCREEN_CAPTURE, payload.screenCaptureDisabled),
            PolicyType.USB_FILE_TRANSFER to policyHandler.setPolicy(PolicyType.USB_FILE_TRANSFER, payload.usbFileTransferDisabled),
            PolicyType.SAFE_BOOT to policyHandler.setPolicy(PolicyType.SAFE_BOOT, payload.safeBootDisabled),
            PolicyType.ADD_USER to policyHandler.setPolicy(PolicyType.ADD_USER, payload.addUserDisabled),
            PolicyType.OUTGOING_CALLS to policyHandler.setPolicy(PolicyType.OUTGOING_CALLS, payload.outgoingCallsDisabled),
            PolicyType.SMS to policyHandler.setPolicy(PolicyType.SMS, payload.smsDisabled)
        )

        val failed = results.filter { !it.second }.map { it.first }

        Logger.i(
            "Applied backend policy: camera=${payload.cameraDisabled}, " +
                "factoryReset=${payload.factoryResetDisabled}, " +
                "screenCapture=${payload.screenCaptureDisabled}, " +
                "usbFileTransfer=${payload.usbFileTransferDisabled}, " +
                "safeBoot=${payload.safeBootDisabled}, " +
                "addUser=${payload.addUserDisabled}, " +
                "outgoingCalls=${payload.outgoingCallsDisabled}, " +
                "sms=${payload.smsDisabled}"
        )

        if (failed.isNotEmpty()) {
            Logger.w(
                "Backend policy check-in reported success but these policies were NOT enforced " +
                    "on-device (device admin inactive or app is not Device Owner): $failed"
            )
        }

        applyKioskMode(payload.kioskMode)
        applyPasswordPolicy(payload.passwordPolicy)
        applyAppRestrictions(payload.appRestrictions)
        applyWifiConfig(payload.wifiConfig)
    }

    private fun applyPasswordPolicy(passwordPolicy: PasswordPolicyPayloadDto?) {
        if (passwordPolicy == null) return

        passwordPolicy.quality?.let { qualityName ->
            val quality = PasswordQualityResolver.resolve(qualityName)
            if (quality != null) {
                val success = deviceAdminManager.setPasswordQuality(quality)
                Logger.i("Applied backend passwordPolicy.quality=$qualityName ($quality), success=$success")
            }
        }

        passwordPolicy.minimumLength?.let { length ->
            val success = deviceAdminManager.setPasswordMinimumLength(length)
            Logger.i("Applied backend passwordPolicy.minimumLength=$length, success=$success")
        }

        passwordPolicy.maxFailedAttemptsBeforeWipe?.let { count ->
            val success = deviceAdminManager.setMaximumFailedPasswordsForWipe(count)
            Logger.i("Applied backend passwordPolicy.maxFailedAttemptsBeforeWipe=$count, success=$success")
        }
    }

    private fun applyAppRestrictions(appRestrictions: List<AppRestrictionPayloadDto>) {
        if (appRestrictions.isEmpty()) return

        val failed = appRestrictions.filterNot { appRestrictionsManager.apply(it) }

        Logger.i("Applied backend appRestrictions for ${appRestrictions.size} package(s)")
        if (failed.isNotEmpty()) {
            Logger.w("Backend appRestrictions NOT fully enforced for: ${failed.map { it.packageName }}")
        }
    }

    private fun applyWifiConfig(wifiConfig: WifiConfigPayloadDto?) {
        if (wifiConfig == null) return

        val success = networkManager.connectToWifiNetwork(
            ssid = wifiConfig.ssid,
            securityType = wifiConfig.securityType,
            password = wifiConfig.password,
            hidden = wifiConfig.hidden
        )
        Logger.i("Applied backend wifi config ssid=${wifiConfig.ssid}, success=$success")
    }

    private fun applyKioskMode(kioskMode: KioskModePayloadDto?) {
        if (kioskMode == null) return

        if (!kioskMode.enabled) {
            val success = kioskManager.disabledKioskMode()
            Logger.i("Applied backend kioskMode: enabled=false, success=$success")
            return
        }

        if (kioskMode.allowedPackageNames.isEmpty()) {
            Logger.w(
                "Backend kioskMode.enabled=true but allowedPackageNames is empty - " +
                    "refusing to lock the device to nothing. Not applying."
            )
            return
        }

        // Sets the OS-level lock task allow-list immediately (a DevicePolicyManager
        // call, works from the background). This app only actually enters lock
        // task mode itself (KioskController.startKiosk() -> Activity.startLockTask())
        // the next time MainActivity is foregrounded and sees kiosk enabled, and
        // only if its own package is among allowedPackageNames - startLockTask()
        // cannot be called from this background check-in path.
        val success = kioskManager.enabledKioskMode(kioskMode.allowedPackageNames)
        Logger.i(
            "Applied backend kioskMode: enabled=true, allowedPackageNames=${kioskMode.allowedPackageNames}, " +
                "success=$success"
        )
    }
}
