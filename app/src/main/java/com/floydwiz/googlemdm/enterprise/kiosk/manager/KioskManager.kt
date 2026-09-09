package com.floydwiz.googlemdm.enterprise.kiosk.manager

import android.content.Context
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.kiosk.KioskPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KioskManager @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager,
    @ApplicationContext private val context: Context,
    private val kioskPreferences: KioskPreferences
) {
    private val _kioskModeState = MutableStateFlow(kioskPreferences.isKioskEnabled())

    /** Live kiosk on/off state - lets the UI (AppNavigation) switch to the kiosk screen immediately. */
    val kioskModeState: StateFlow<Boolean> = _kioskModeState.asStateFlow()

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
            _kioskModeState.value = true
        }
        return success
    }

    fun disabledKioskMode(): Boolean {
        val success =deviceAdminManager.clearLockTaskPackages()

        Logger.d("Disabled Kiosk Mode = $success")
        if(success) {
            kioskPreferences.setKioskEnabled(false)
            _kioskModeState.value = false
        }
        return success
    }

    fun isKioskModeEnabled(): Boolean {
        return _kioskModeState.value
    }

    fun isPackageAllowed(
        packages: String
    ): Boolean {
        return deviceAdminManager.isLockTaskPermitted(packages)
    }
}