package com.floydwiz.googlemdm.enterprise.kiosk.manager

import android.content.Context
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.kiosk.KioskPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KioskManager @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager,
    @ApplicationContext private val context: Context,
    private val kioskPreferences: KioskPreferences
) {
    private val _kioskModeChanges = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    /**
     * Emits whenever kiosk mode is enabled/disabled - including when a
     * backend check-in applies it from a background WorkManager coroutine,
     * which can't call Activity.startLockTask() itself. MainActivity
     * collects this to engage/release lock task immediately if it's already
     * in the foreground when the change happens (onResume alone only
     * catches the next foreground *transition*, not a change that arrives
     * while already resumed).
     */
    val kioskModeChanges: SharedFlow<Boolean> = _kioskModeChanges.asSharedFlow()

    /**
     * @param allowedPackageNames Packages permitted to enter lock task mode.
     * Defaults to this DPC app itself, matching the Dashboard's manual
     * toggle. A backend-driven policy can instead pass a specific business
     * app's package name(s) to lock the device to that app - though this
     * app will only actually enter lock task mode itself (via
     * KioskController.startKiosk()) when its own package is included here.
     */
    fun enabledKioskMode(allowedPackageNames: List<String> = listOf(context.packageName)): Boolean {
        val success = deviceAdminManager.setLockTaskPackages(
            allowedPackageNames
        )

        Logger.d("Enabled Kiosk Mode for $allowedPackageNames = $success")
        if(success) {
            kioskPreferences.setKioskEnabled(true)
            _kioskModeChanges.tryEmit(true)
        }
        return success
    }

    fun disabledKioskMode(): Boolean {
        val success =deviceAdminManager.clearLockTaskPackages()

        Logger.d("Disabled Kiosk Mode = $success")
        if(success) {
            kioskPreferences.setKioskEnabled(false)
            _kioskModeChanges.tryEmit(false)
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