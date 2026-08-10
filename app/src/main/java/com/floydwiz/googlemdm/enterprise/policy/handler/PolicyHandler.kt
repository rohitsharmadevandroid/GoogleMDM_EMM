package com.floydwiz.googlemdm.enterprise.policy.handler

import com.floydwiz.googlemdm.enterprise.policy.manager.EnterprisePolicyManager
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyHandler @Inject constructor(
    private val enterprisePolicyManager: EnterprisePolicyManager
) {
    fun setPolicy(
        type: PolicyType,
        disabled: Boolean
    ): Boolean {
        return when(type) {
            PolicyType.CAMERA -> enterprisePolicyManager.setCameraDisabled(disabled)
            PolicyType.SCREEN_CAPTURE -> enterprisePolicyManager.setScreenCaptureDisabled(disabled)
            PolicyType.USB_FILE_TRANSFER -> enterprisePolicyManager.setUsbFileTransferDisabled(disabled)
            PolicyType.SAFE_BOOT -> enterprisePolicyManager.setSafeBootDisabled(disabled)
            PolicyType.FACTORY_RESET -> enterprisePolicyManager.setFactoryResetDisabled(disabled)
            PolicyType.ADD_USER -> enterprisePolicyManager.setAddUserDisabled(disabled)
            PolicyType.OUTGOING_CALLS -> enterprisePolicyManager.setOutgoingCallDisabled(disabled)
            PolicyType.SMS -> enterprisePolicyManager.setSMSDisabled(disabled)
            PolicyType.KIOSK -> false //KIOSK handle separately
            else -> false
        }
    }

    fun getPolicy(
        type: PolicyType
    ): Boolean {
        return when(type) {
            PolicyType.CAMERA -> enterprisePolicyManager.getCameraDisabled()
            PolicyType.SCREEN_CAPTURE -> enterprisePolicyManager.getScreenCaptureDisabled()
            PolicyType.USB_FILE_TRANSFER -> enterprisePolicyManager.getUsbFileTransferDisabled()
            PolicyType.SAFE_BOOT -> enterprisePolicyManager.getSafeBootDisabled()
            PolicyType.FACTORY_RESET -> enterprisePolicyManager.getFactoryResetDisabled()
            PolicyType.ADD_USER -> enterprisePolicyManager.getAddUserDisabled()
            PolicyType.OUTGOING_CALLS -> enterprisePolicyManager.getOutgoingCallDisabled()
            PolicyType.SMS -> enterprisePolicyManager.getSMSDisabled()
            PolicyType.KIOSK -> false //KIOSK handle separately
            else -> false
        }
    }
}