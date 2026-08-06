package com.floydwiz.googlemdm.presentation.viewmodel

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.policy.manager.EnterprisePolicyManager
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType
import com.floydwiz.googlemdm.enterprise.policy.registry.PolicyDefinitions
import com.floydwiz.googlemdm.presentation.state.DashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager,
    private val enterprisePolicyManager: EnterprisePolicyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDeviceStatus()
    }

    fun loadDeviceStatus() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val cameraDisabled = enterprisePolicyManager.getCameraDisabled()
            val screenCaptureDisabled = enterprisePolicyManager.getScreenCaptureDisabled()
            val usbFileTransferDisabled = enterprisePolicyManager.getUsbFileTransferDisabled()
            val safeBootDisabled = enterprisePolicyManager.getSafeBootDisabled()
            val factoryResetDisabled = enterprisePolicyManager.getFactoryResetDisabled()
            val addUserDisabled = enterprisePolicyManager.getAddUserDisabled()
            val outgoingCallsDisabled = enterprisePolicyManager.getOutgoingCallDisabled()
            val smsDisabled = enterprisePolicyManager.getSMSDisabled()
            val policies = listOf(
                PolicyDefinitions.createPolicy(
                    PolicyDefinitions.Camera,
                    cameraDisabled
                ),
                PolicyDefinitions.createPolicy(
                    PolicyDefinitions.ScreenCapture,
                    screenCaptureDisabled
                ),
                PolicyDefinitions.createPolicy(
                    PolicyDefinitions.UsbFileTransfer,
                    usbFileTransferDisabled
                ),
                PolicyDefinitions.createPolicy(
                    PolicyDefinitions.SafeBoot,
                    safeBootDisabled
                ),
                PolicyDefinitions.createPolicy(
                    PolicyDefinitions.FactoryReset,
                    factoryResetDisabled
                ),
                PolicyDefinitions.createPolicy(
                    PolicyDefinitions.addUser,
                    addUserDisabled
                ),
                PolicyDefinitions.createPolicy(
                    PolicyDefinitions.outgoingCalls,
                    outgoingCallsDisabled
                ),
                PolicyDefinitions.createPolicy(
                    PolicyDefinitions.SMS,
                    smsDisabled
                )
            )
            _uiState.value = DashboardUiState(
                isAdminActive = deviceAdminManager.isAdminActive(),
                isDeviceOwner = deviceAdminManager.isDeviceOwner(),

                manufacture = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,

                isLoading = false,

                policies = policies
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
        }
    }

    fun createAdminIntent(): Intent {
        Logger.d("DashboardViewModel createAdminIntent: Creating admin intent")
        return deviceAdminManager.createAdminIntent()
    }

    fun setPolicy(type: PolicyType, enabled: Boolean) {
        when (type) {
            PolicyType.CAMERA -> setCameraDisabled(enabled)
            PolicyType.SCREEN_CAPTURE -> setScreenCaptureDisabled(enabled)
            PolicyType.USB_FILE_TRANSFER -> setUsbFileTransferDisabled(enabled)
            PolicyType.SAFE_BOOT -> setSafeBootDisabled(enabled)
            PolicyType.FACTORY_RESET -> setFactoryResetDisabled(enabled)
            PolicyType.ADD_USER -> setAddUserDisabled(enabled)
            PolicyType.OUTGOING_CALLS -> setOutgoingCallsDisabled(enabled)
            PolicyType.SMS -> setSMSDisabled(enabled)
            else -> {}
        }
    }

    fun setCameraDisabled(disabled: Boolean) {
        val success = enterprisePolicyManager.setCameraDisabled(disabled)

        if(success) {
            _uiState.update { state ->
                state.copy(
                    policies = state.policies.map { policy ->
                        if (policy.type == PolicyType.CAMERA) {
                            policy.copy(enabled = disabled)
                        }
                        else {
                            policy
                        }
                    }
                )
            }
        }
    }

    fun setScreenCaptureDisabled(disabled: Boolean) {
        val success = enterprisePolicyManager.setScreenCaptureDisabled(disabled)

        if (success) {
            _uiState.update { state ->
                state.copy(
                    policies = state.policies.map { policy ->
                        if (policy.type == PolicyType.SCREEN_CAPTURE) {
                            policy.copy(enabled = disabled)
                        } else {
                            policy
                        }
                    }
                )
            }
        }
    }

    fun setUsbFileTransferDisabled(disabled: Boolean) {
        val success = enterprisePolicyManager.setUsbFileTransferDisabled(disabled)

        if (success) {
            _uiState.update { state ->
                state.copy(
                    policies = state.policies.map { policy ->
                        if (policy.type == PolicyType.USB_FILE_TRANSFER) {
                            policy.copy(enabled = disabled)
                        } else {
                            policy
                        }
                    }
                )
            }
        }
    }

    fun setSafeBootDisabled(disabled: Boolean) {
        val success = enterprisePolicyManager.setSafeBootDisabled(disabled)

        if (success) {
            _uiState.update { state ->
                state.copy(
                    policies = state.policies.map { policy ->
                        if (policy.type == PolicyType.SAFE_BOOT) {
                            policy.copy(enabled = disabled)
                        } else {
                            policy
                        }
                    }
                )
            }
        }
    }

    fun setFactoryResetDisabled(disabled: Boolean) {
        val success = enterprisePolicyManager.setFactoryResetDisabled(disabled)

        if (success) {
            _uiState.update { state ->
                state.copy(
                    policies = state.policies.map { policy ->
                        if (policy.type == PolicyType.FACTORY_RESET) {
                            policy.copy(enabled = disabled)
                        } else {
                            policy
                        }
                    }
                )
            }
        }
    }

    fun setAddUserDisabled(disabled: Boolean) {
        val success = enterprisePolicyManager.setAddUserDisabled(disabled)

        if (success) {
            _uiState.update { state ->
                state.copy(
                    policies = state.policies.map { policy ->
                        if (policy.type == PolicyType.ADD_USER) {
                            policy.copy(enabled = disabled)
                        } else {
                            policy
                        }
                    }
                )
            }
        }
    }

    fun setOutgoingCallsDisabled(disabled: Boolean) {
        val success = enterprisePolicyManager.setOutgoingCallDisabled(disabled)

        if (success) {
            _uiState.update { state ->
                state.copy(
                    policies = state.policies.map { policy ->
                        if (policy.type == PolicyType.OUTGOING_CALLS) {
                            policy.copy(enabled = disabled)
                        } else {
                            policy
                        }
                    }
                )
            }
        }
    }

    fun setSMSDisabled(disabled: Boolean) {
        val success = enterprisePolicyManager.setSMSDisabled(disabled)

        if(success) {
            _uiState.update { state ->
                state.copy(
                    policies = state.policies.map { policy ->
                        if (policy.type == PolicyType.SMS) {
                            policy.copy(enabled = disabled)
                        } else {
                            policy
                        }
                    }
                )
            }
        }
    }
}