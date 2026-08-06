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
    fun getCameraDisabled(): Boolean {
        return deviceAdminManager.getCameraDisabled()
    }

    fun setScreenCaptureDisabled(disabled: Boolean): Boolean {
        return deviceAdminManager.setScreenCaptureDisabled(disabled)
    }
    fun getScreenCaptureDisabled(): Boolean {
        return deviceAdminManager.getScreenCaptureDisabled()
    }

    fun setUsbFileTransferDisabled(disabled: Boolean): Boolean {
        return deviceAdminManager.setUsbFileTransferDisabled(disabled)
    }

    fun getUsbFileTransferDisabled(): Boolean {
        return deviceAdminManager.getUsbFileTransferDisabled()
    }

    fun setSafeBootDisabled(disabled: Boolean): Boolean {
        return deviceAdminManager.setSafeBootDisabled(disabled)
    }

    fun getSafeBootDisabled(): Boolean {
        return deviceAdminManager.getSafeBootDisabled()
    }

    fun setFactoryResetDisabled(disabled: Boolean): Boolean {
        return deviceAdminManager.setFactoryResetDisabled(disabled)
    }

    fun getFactoryResetDisabled(): Boolean {
        return deviceAdminManager.getFactoryResetDisabled()
    }

    fun setAddUserDisabled(disabled: Boolean): Boolean {
        return deviceAdminManager.setAddUserDisabled(disabled)
    }

    fun getAddUserDisabled(): Boolean {
        return deviceAdminManager.getAddUserDisabled()
    }

    fun setOutgoingCallDisabled(disabled: Boolean): Boolean {
        return deviceAdminManager.setOutgoingCallsDisabled(disabled)
    }

    fun getOutgoingCallDisabled(): Boolean {
        return deviceAdminManager.getOutgoingCallsDisabled()
    }

    fun setSMSDisabled(disabled: Boolean): Boolean {
        return deviceAdminManager.setSMSDisabled(disabled)
    }

    fun getSMSDisabled(): Boolean {
        return deviceAdminManager.getSMSDisabled()
    }
}