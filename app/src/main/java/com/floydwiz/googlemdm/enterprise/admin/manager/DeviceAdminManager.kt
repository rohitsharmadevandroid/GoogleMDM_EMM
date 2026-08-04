package com.floydwiz.googlemdm.enterprise.admin.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

    fun isAdminActive(): Boolean {
        val active = dpm.isAdminActive(adminComponent)
        Logger.d("Admin Active = $active")
        return active
    }

    fun isDeviceOwner(): Boolean {
        val owner = dpm.isDeviceOwnerApp(context.packageName)
        Logger.d("Device Owner = $owner")
        return owner
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

    fun isCameraDisabled(): Boolean {
        if(!isAdminActive())
        {
            return false
        }

        val disabled = dpm.getCameraDisabled(adminComponent)
        Logger.d("Camera Disabled Status = $disabled")
        return disabled
    }
}