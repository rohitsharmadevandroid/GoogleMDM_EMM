package com.floydwiz.googlemdm.enterprise.policy.manager

import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class EnterprisePolicyManager @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager
) {
    fun setCameraDisabled(disabled: Boolean): Boolean {
        return deviceAdminManager.setCameraDisabled(disabled)
    }
    fun isCameraDisabled(): Boolean {
        return deviceAdminManager.isCameraDisabled()
    }
}