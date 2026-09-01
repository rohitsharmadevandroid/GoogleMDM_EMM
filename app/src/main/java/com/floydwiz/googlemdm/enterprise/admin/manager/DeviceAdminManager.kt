package com.floydwiz.googlemdm.enterprise.admin.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import com.floydwiz.googlemdm.BuildConfig
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.receiver.MyDeviceAdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.FileNotFoundException
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class DeviceAdminManager @Inject constructor(
    @ApplicationContext private val context : Context
) {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)

    private val adminComponent =
        ComponentName(
            context,
            MyDeviceAdminReceiver::class.java
        )

    private fun setUserRestriction(
        restriction: String,
        disabled: Boolean
    ): Boolean {

        if(!isAdminActive()) {
            Logger.w("Cannot Change User Restriction. Device Admin is not active")
            return false
        }

        return try{
            if(disabled) {
                dpm.addUserRestriction(
                    adminComponent,
                    restriction
                )
            } else {
                dpm.clearUserRestriction(
                    adminComponent,
                    restriction
                )
            }
            Logger.i("User Restriction Disabled Status = $disabled")
            true
        } catch (e: SecurityException) {
            Logger.e("Failed to change user restriction")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while changing user restriction: $restriction")
            false
        }
    }

    private fun getUserRestriction(
        restriction: String
    ): Boolean {

        if (!isAdminActive()) {
            Logger.w("Cannot Check User Restriction. Device Admin is not active")
            return false
        }
        return try {
            val restrictions = dpm.getUserRestrictions(adminComponent)
            restrictions.getBoolean(restriction)
        } catch (e: SecurityException) {
            Logger.e("Failed to read user restriction")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while reading user restriction: $restriction")
            false
        }
    }

    fun isAdminActive(): Boolean {
        return try {
            val active = dpm.isAdminActive(adminComponent)
            Logger.d("Device Admin Active = $active")
            active
        } catch (e: SecurityException) {
            Logger.e("Failed to check device admin status")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while checking device admin status")
            false
        }
    }

    fun isDeviceOwner(): Boolean {
        return try {
            val owner = dpm.isDeviceOwnerApp(context.packageName)
            Logger.d("Device Owner = $owner")
            owner
        } catch (e: SecurityException) {
            Logger.e("Failed to check Device Owner status")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while checking Device Owner status")
            false
        }
    }

    fun getAdminComponent(): ComponentName {
        Logger.d("Admin Component = $adminComponent")
        return adminComponent
    }

    fun createAdminIntent(): Intent {
        Logger.d("Creating Device Admin Activation Intent")
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Google MDM requires Device Administrator permission to manage enterprise devices.")
        }
    }

    fun setCameraDisabled(disabled: Boolean): Boolean {

        if(!isAdminActive()) {
            Logger.w("Cannot Change Camera Policy. Device Admin is not active")
            return false
        }
        return try {
            dpm.setCameraDisabled(adminComponent,disabled) // here internally android storing the policy of camera ,, Disabled = true
            Logger.d("Camera Disabled Status = $disabled")
            true
        } catch (e: SecurityException) {
            Logger.e("Failed to change camera policy")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while changing camera policy")
            false
        }
    }

    fun getCameraDisabled(): Boolean {
        if(!isAdminActive())
        {
            return false
        }

        return try {
            val disabled = dpm.getCameraDisabled(adminComponent)
            Logger.d("Camera Disabled Status = $disabled")
            disabled
        } catch (e: SecurityException) {
            Logger.e("Failed to read camera policy")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while reading camera policy")
            false
        }
    }

    fun setScreenCaptureDisabled(disabled: Boolean): Boolean {

        if(!isAdminActive()){
            Logger.w("Cannot Change Screen Capture Policy. Device Admin is not active")
            return false
        }

        return try {
            dpm.setScreenCaptureDisabled(adminComponent,disabled)
            Logger.d("Screen Capture Disabled Status = $disabled")
            true
        } catch (e: SecurityException) {
            Logger.e("Failed to change screen capture policy")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while changing screen capture policy")
            false
        }
    }

    fun getScreenCaptureDisabled(): Boolean {
        if(!isAdminActive())
        {
            return false
        }
        return try {
            val disabled = dpm.getScreenCaptureDisabled(adminComponent)
            Logger.d("Screen Capture Disabled Status = $disabled")
            disabled
        } catch (e: SecurityException) {
            Logger.e("Failed to read screen capture policy")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while reading screen capture policy")
            false
        }
    }

    fun setUsbFileTransferDisabled(disabled: Boolean): Boolean {
        return setUserRestriction(
            UserManager.DISALLOW_USB_FILE_TRANSFER,
            disabled
        )
    }

    fun getUsbFileTransferDisabled(): Boolean {
        return getUserRestriction(
            UserManager.DISALLOW_USB_FILE_TRANSFER
        )
    }

    fun setSafeBootDisabled(disabled: Boolean): Boolean {
        return setUserRestriction(
            UserManager.DISALLOW_SAFE_BOOT,
            disabled
        )
    }

    fun getSafeBootDisabled(): Boolean {
        return getUserRestriction(
            UserManager.DISALLOW_SAFE_BOOT
        )
    }

    fun setFactoryResetDisabled(disabled: Boolean): Boolean{
        return setUserRestriction(
            UserManager.DISALLOW_FACTORY_RESET,
            disabled
        )
    }

    fun getFactoryResetDisabled(): Boolean {
        return getUserRestriction(
            UserManager.DISALLOW_FACTORY_RESET
        )
    }

    fun setAddUserDisabled(disabled: Boolean): Boolean {
        return setUserRestriction(
            UserManager.DISALLOW_ADD_USER,
            disabled
        )
    }

    fun getAddUserDisabled(): Boolean {
        return getUserRestriction(
            UserManager.DISALLOW_ADD_USER
        )
    }

    fun setOutgoingCallsDisabled(disabled: Boolean): Boolean {
        return setUserRestriction(
            UserManager.DISALLOW_OUTGOING_CALLS,
            disabled
        )
    }

    fun getOutgoingCallsDisabled(): Boolean {
        return getUserRestriction(
            UserManager.DISALLOW_OUTGOING_CALLS
        )
    }

    fun setSMSDisabled(disabled: Boolean):Boolean {
        return setUserRestriction(
            UserManager.DISALLOW_SMS,
            disabled
        )
    }

    fun getSMSDisabled(): Boolean {
        return getUserRestriction(
            UserManager.DISALLOW_SMS
        )
    }

    //Kiosk Mode Started here
    fun setLockTaskPackages(
        packages: List<String>
    ): Boolean {
        if(!isDeviceOwner()) {
            Logger.e("Cannot set Lock Task Packages. App is not Device Owner")
            return false
        }

        return try {
            dpm.setLockTaskPackages(
                adminComponent,
                packages.toTypedArray()
            )
            Logger.d("Lock Task Packages set: $packages")
            true
        } catch (e: Exception) {
            Logger.e("Failed to set Lock Task Packages")
            false
        }
    }

    fun getLockTaskPackages(): Array<String> {
        return try {
            val packages = dpm.getLockTaskPackages(adminComponent)
            Logger.d("Lock Task Packages: $packages")
            packages
        } catch (e: SecurityException) {
            Logger.e("Failed to read Lock Task Packages")
            emptyArray()
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while reading Lock Task Packages")
            emptyArray()
        }
    }

    fun isLockTaskPermitted(
        packageName: String
    ): Boolean {
        return try {
            val permitted = dpm.isLockTaskPermitted(packageName)

            Logger.d("Lock Task permitted for $packageName = $permitted")

            permitted
        } catch (e: SecurityException) {
            Logger.e("Failed to check Lock Task permission")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while checking Lock Task permission")
            false
        }
    }

    fun clearLockTaskPackages(): Boolean {
        if(!isDeviceOwner()) {
            Logger.e("Cannot clear Lock Task Packages. App is Not Device Owner")
            return false
        }

        return try {
            dpm.setLockTaskPackages(
                adminComponent,
                emptyArray()
            )
            Logger.d("Lock Task Packages cleared")
            true
        } catch (e: Exception) {
            Logger.e("Failed to load Lock Task Packages")
            false
        }
    }

    fun setWifiConfigDisabled(disabled: Boolean):Boolean {
        return setUserRestriction(
            UserManager.DISALLOW_CONFIG_WIFI,
            disabled
        )
    }

    fun getWifiConfigDisabled(): Boolean{
        return getUserRestriction(
            UserManager.DISALLOW_CONFIG_WIFI
        )
    }

    /**
     * Trusts a CA certificate device-wide. Device owners can do this without
     * any user interaction, unlike the Settings > Install a certificate flow.
     */
    fun installCaCertificate(certBytes: ByteArray): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot install CA certificate. App is not Device Owner")
            return false
        }

        return try {
            val installed = dpm.installCaCert(adminComponent, certBytes)
            Logger.i("CA certificate install result = $installed")
            installed
        } catch (e: SecurityException) {
            Logger.e("Failed to install CA certificate: ${e.message}")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while installing CA certificate: ${e.message}")
            false
        }
    }

    /**
     * Debug-build-only convenience: installs the local EMM backend's dev CA
     * certificate (app/src/debug/assets/emm_dev_cert.der), so the local dev
     * TLS setup doesn't depend on the Settings > Install a certificate flow,
     * which is unreliable on several Android 13+ builds. The asset does not
     * exist in release builds, so this is a no-op there.
     */
    fun installBundledDevCaCertificate(): Boolean {
        if (!BuildConfig.DEBUG) return false

        val certBytes = try {
            context.assets.open(DEV_CA_CERT_ASSET_NAME).use { it.readBytes() }
        } catch (e: FileNotFoundException) {
            Logger.w("No bundled dev CA cert asset found: $DEV_CA_CERT_ASSET_NAME")
            return false
        } catch (e: Exception) {
            Logger.e("Failed to read bundled dev CA cert: ${e.message}")
            return false
        }

        return installCaCertificate(certBytes)
    }

    /**
     * Device Owner apps can grant themselves a runtime permission without a
     * user-facing prompt - used before reads that require e.g. READ_PHONE_STATE
     * (IMEI) for the REQUEST_DEVICE_INFO command.
     */
    fun grantSelfRuntimePermission(permission: String): Boolean {
        if (!isDeviceOwner()) {
            Logger.w("Cannot grant self permission $permission. App is not Device Owner")
            return false
        }

        return try {
            dpm.setPermissionGrantState(
                adminComponent,
                context.packageName,
                permission,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
        } catch (e: SecurityException) {
            Logger.e("Failed to grant self permission $permission")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while granting self permission $permission")
            false
        }
    }

    //Backend command execution started here
    fun lockNow(): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot Lock Device. App is not Device Owner")
            return false
        }

        return try {
            dpm.lockNow()
            Logger.i("Device locked")
            true
        } catch (e: SecurityException) {
            Logger.e("Failed to lock device")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while locking device")
            false
        }
    }

    /**
     * resetPassword() has been deprecated since API 26 and is rejected by
     * many modern Android versions for a Device Owner when the user already
     * has a lock screen credential - a false/exception result here is an
     * honest failure, not a sign something else is wrong.
     */
    fun resetPassword(newPassword: String, flags: Int): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot Reset Password. App is not Device Owner")
            return false
        }

        return try {
            @Suppress("DEPRECATION")
            val result = dpm.resetPassword(newPassword, flags)
            Logger.i("Reset Password result = $result")
            result
        } catch (e: SecurityException) {
            Logger.e("Failed to reset password")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while resetting password")
            false
        }
    }

    /** dpm.wipeData() tears the device down essentially immediately - callers must ack before invoking this. */
    fun wipeData(flags: Int): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot Wipe Device. App is not Device Owner")
            return false
        }

        return try {
            dpm.wipeData(flags)
            Logger.i("Wipe Data requested with flags = $flags")
            true
        } catch (e: SecurityException) {
            Logger.e("Failed to wipe device")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while wiping device")
            false
        }
    }

    /** dpm.reboot() restarts the device essentially immediately - callers must ack before invoking this. */
    fun rebootDevice(): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot Reboot Device. App is not Device Owner")
            return false
        }

        return try {
            dpm.reboot(adminComponent)
            Logger.i("Reboot requested")
            true
        } catch (e: SecurityException) {
            Logger.e("Failed to reboot device")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while rebooting device")
            false
        }
    }

    /** clearApplicationUserData() is only available to a Device Owner from API 28 onward. */
    suspend fun clearApplicationData(packageName: String): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot Clear App Data. App is not Device Owner")
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Logger.w("Cannot Clear App Data. Requires API 28+, running ${Build.VERSION.SDK_INT}")
            return false
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                dpm.clearApplicationUserData(
                    adminComponent,
                    packageName,
                    Executor { it.run() },
                    { _, succeeded ->
                        Logger.i("Clear App Data for $packageName succeeded = $succeeded")
                        continuation.resume(succeeded)
                    }
                )
            }
        } catch (e: SecurityException) {
            Logger.e("Failed to clear app data for $packageName")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while clearing app data for $packageName")
            false
        }
    }

    //Password policy started here
    // setPasswordQuality/setPasswordMinimumLength are deprecated in favor of
    // setRequiredPasswordComplexity() on newer Android, but that's a coarser
    // 4-tier enum that can't represent the backend's exact minimumLength/
    // quality contract - keeping the detailed API is deliberate, matching
    // how resetPassword()'s deprecation was already handled this session.
    @Suppress("DEPRECATION")
    fun setPasswordQuality(quality: Int): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot Set Password Quality. App is not Device Owner")
            return false
        }

        return try {
            dpm.setPasswordQuality(adminComponent, quality)
            Logger.i("Password Quality set = $quality")
            true
        } catch (e: SecurityException) {
            Logger.e("Failed to set password quality")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while setting password quality")
            false
        }
    }

    @Suppress("DEPRECATION")
    fun setPasswordMinimumLength(length: Int): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot Set Password Minimum Length. App is not Device Owner")
            return false
        }

        return try {
            dpm.setPasswordMinimumLength(adminComponent, length)
            Logger.i("Password Minimum Length set = $length")
            true
        } catch (e: SecurityException) {
            Logger.e("Failed to set password minimum length")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while setting password minimum length")
            false
        }
    }

    fun setMaximumFailedPasswordsForWipe(count: Int): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot Set Maximum Failed Passwords For Wipe. App is not Device Owner")
            return false
        }

        return try {
            dpm.setMaximumFailedPasswordsForWipe(adminComponent, count)
            Logger.i("Maximum Failed Passwords For Wipe set = $count")
            true
        } catch (e: SecurityException) {
            Logger.e("Failed to set maximum failed passwords for wipe")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while setting maximum failed passwords for wipe")
            false
        }
    }

    //App restrictions started here
    fun setApplicationHidden(packageName: String, hidden: Boolean): Boolean {
        if (!isDeviceOwner()) {
            Logger.e("Cannot Change App Hidden State. App is not Device Owner")
            return false
        }

        return try {
            val success = dpm.setApplicationHidden(adminComponent, packageName, hidden)
            Logger.i("Application Hidden Status for $packageName = $hidden, success=$success")
            success
        } catch (e: SecurityException) {
            Logger.e("Failed to change hidden state for $packageName")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while changing hidden state for $packageName")
            false
        }
    }

    /**
     * Device Owner is exempt from Android's package-visibility restrictions,
     * so this can query any installed package without a <queries> manifest
     * declaration.
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while checking if $packageName is installed")
            false
        }
    }

    companion object {
        private const val DEV_CA_CERT_ASSET_NAME = "emm_dev_cert.der"
    }
}