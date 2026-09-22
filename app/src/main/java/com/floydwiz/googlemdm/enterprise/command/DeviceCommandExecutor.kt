package com.floydwiz.googlemdm.enterprise.command

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.data.local.DeviceInfoProvider
import com.floydwiz.googlemdm.data.remote.CommandDto
import com.floydwiz.googlemdm.data.remote.CommandParamsDto
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.command.model.CommandOutcome
import com.floydwiz.googlemdm.enterprise.command.model.CommandType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCommandExecutor @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager,
    private val deviceInfoProvider: DeviceInfoProvider,
    @ApplicationContext private val context: Context
) : CommandExecutor {

    override suspend fun execute(command: CommandDto): CommandOutcome {
        val type = try {
            CommandType.valueOf(command.type)
        } catch (e: IllegalArgumentException) {
            Logger.w("Unknown command type: ${command.type}")
            return CommandOutcome.Failed("Unknown command type: ${command.type}")
        }

        return when (type) {
            CommandType.LOCK -> executeLock(command.params)
            CommandType.RESET_PASSWORD -> executeResetPassword(command.params)
            CommandType.CLEAR_APP_DATA -> executeClearAppData(command.params)
            CommandType.REQUEST_DEVICE_INFO -> executeRequestDeviceInfo(command.params)
            CommandType.WIPE -> executeWipe(command.params)
            CommandType.REBOOT -> executeReboot()
        }
    }

    private fun executeLock(params: CommandParamsDto): CommandOutcome {
        // AOSP DevicePolicyManager.lockNow() has no duration parameter, so
        // lockDurationSeconds cannot currently be enforced client-side.
        params.lockDurationSeconds?.let {
            Logger.w("LOCK requested lockDurationSeconds=$it, but lockNow() has no duration support - ignoring")
        }

        return if (deviceAdminManager.lockNow()) {
            CommandOutcome.Completed()
        } else {
            CommandOutcome.Failed("lockNow() failed - see logs")
        }
    }

    private fun executeResetPassword(params: CommandParamsDto): CommandOutcome {
        val newPassword = params.newPassword
            ?: return CommandOutcome.Failed("RESET_PASSWORD command missing newPassword")

        val flags = mapResetPasswordFlags(params.resetPasswordFlags)

        return if (deviceAdminManager.resetPassword(newPassword, flags)) {
            CommandOutcome.Completed()
        } else {
            CommandOutcome.Failed("resetPassword() failed - rejected by platform or see logs")
        }
    }

    private suspend fun executeClearAppData(params: CommandParamsDto): CommandOutcome {
        if (params.clearAppsDataPackageNames.isEmpty()) {
            return CommandOutcome.Failed("CLEAR_APP_DATA command missing clearAppsDataPackageNames")
        }

        val failedPackages = params.clearAppsDataPackageNames.filterNot { packageName ->
            deviceAdminManager.clearApplicationData(packageName)
        }

        return if (failedPackages.isEmpty()) {
            CommandOutcome.Completed()
        } else {
            CommandOutcome.Failed("Failed to clear app data for: $failedPackages")
        }
    }

    private fun executeRequestDeviceInfo(params: CommandParamsDto): CommandOutcome {
        // requestDeviceInfoType isn't filtered on yet - its accepted values
        // aren't confirmed against the backend, so every field below is
        // collected regardless of the requested type. Logged for now so a
        // future filter can be added once those values are confirmed.
        Logger.d("REQUEST_DEVICE_INFO requested, requestDeviceInfoType=${params.requestDeviceInfoType}")

        val resultData = mutableMapOf(
            "osVersion" to deviceInfoProvider.osVersion(),
            "model" to deviceInfoProvider.model(),
            "manufacturer" to deviceInfoProvider.manufacturer()
        )

        readBatteryLevel()?.let { resultData["batteryLevel"] = it }
        readImei()?.let { resultData["imei"] = it }

        return CommandOutcome.Completed(resultData = resultData)
    }

    private fun readBatteryLevel(): String? {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 }
                ?.toString()
        } catch (e: Exception) {
            Logger.e("Failed to read battery level: ${e.message}")
            null
        }
    }

    /**
     * getImei() requires READ_PHONE_STATE; a Device Owner can grant that to
     * itself without a user-facing prompt, so it's requested here on demand
     * rather than at provisioning time.
     */
    private fun readImei(): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            deviceAdminManager.grantSelfRuntimePermission(Manifest.permission.READ_PHONE_STATE)
        }

        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telephonyManager?.imei
        } catch (e: SecurityException) {
            Logger.e("Failed to read IMEI: permission not granted")
            null
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while reading IMEI: ${e.message}")
            null
        }
    }

    /** Terminal command - caller must ack before invoking execute() for WIPE. */
    private fun executeWipe(params: CommandParamsDto): CommandOutcome {
        val flags = mapWipeFlags(params.wipeDataFlags)

        return if (deviceAdminManager.wipeData(flags)) {
            CommandOutcome.Completed()
        } else {
            CommandOutcome.Failed("wipeData() failed - see logs")
        }
    }

    /** Terminal command - caller must ack before invoking execute() for REBOOT. */
    private fun executeReboot(): CommandOutcome {
        return if (deviceAdminManager.rebootDevice()) {
            CommandOutcome.Completed()
        } else {
            CommandOutcome.Failed("reboot() failed - see logs")
        }
    }

    override fun isReadyForDestructiveCommand(): Boolean = deviceAdminManager.isDeviceOwner()

    private fun mapWipeFlags(flagNames: List<String>): Int {
        return flagNames.fold(0) { acc, name ->
            val flag = when (name) {
                "WIPE_EXTERNAL_STORAGE" -> DevicePolicyManager.WIPE_EXTERNAL_STORAGE
                "WIPE_RESET_PROTECTION_DATA" -> DevicePolicyManager.WIPE_RESET_PROTECTION_DATA
                "WIPE_EUICC" -> DevicePolicyManager.WIPE_EUICC
                else -> {
                    Logger.w("Unrecognized wipeDataFlags entry: $name - ignoring")
                    0
                }
            }
            acc or flag
        }
    }

    private fun mapResetPasswordFlags(flagNames: List<String>): Int {
        return flagNames.fold(0) { acc, name ->
            @Suppress("DEPRECATION")
            val flag = when (name) {
                "RESET_PASSWORD_REQUIRE_ENTRY" -> DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY
                "RESET_PASSWORD_DO_NOT_ASK_CREDENTIALS_ON_BOOT" ->
                    DevicePolicyManager.RESET_PASSWORD_DO_NOT_ASK_CREDENTIALS_ON_BOOT
                else -> {
                    Logger.w("Unrecognized resetPasswordFlags entry: $name - ignoring")
                    0
                }
            }
            acc or flag
        }
    }
}
