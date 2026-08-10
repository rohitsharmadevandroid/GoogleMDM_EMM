package com.floydwiz.googlemdm.enterprise.kiosk.manager

import android.content.Context
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.kiosk.KioskPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KioskManager @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager,
    @ApplicationContext private val context: Context,
    private val kioskPreferences: KioskPreferences
) {
    fun enabledKioskMode(): Boolean {
        val success =  deviceAdminManager.setLockTaskPackages(
            listOf(context.packageName)
        )

        Logger.d("Enabled Kiosk Mode = $success")
        if(success) {
            kioskPreferences.setKioskEnabled(true)
        }
        return success
    }

    fun disabledKioskMode(): Boolean {
        val success =deviceAdminManager.clearLockTaskPackages()

        Logger.d("Disabled Kiosk Mode = $success")
        if(success) {
            kioskPreferences.setKioskEnabled(false)
        }
        return success
    }

    fun isKioskModeEnabled(): Boolean {
        return kioskPreferences.isKioskEnabled()
    }

    fun isPackageAllowed(
        packages: String
    ): Boolean {
        return deviceAdminManager.isLockTaskPermitted(packages)
    }
}