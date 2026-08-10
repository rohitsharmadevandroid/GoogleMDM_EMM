package com.floydwiz.googlemdm.enterprise.admin.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.receiver.MyDeviceAdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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
}