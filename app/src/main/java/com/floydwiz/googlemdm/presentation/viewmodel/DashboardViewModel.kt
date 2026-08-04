package com.floydwiz.googlemdm.presentation.viewmodel

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.policy.manager.EnterprisePolicyManager
import com.floydwiz.googlemdm.enterprise.policy.model.EnterprisePolicy
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType
import com.floydwiz.googlemdm.presentation.state.DashboardUiState
import com.floydwiz.googlemdm.presentation.state.PolicyUiState
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
            val cameraDisabled = enterprisePolicyManager.isCameraDisabled()
            val policies = listOf(
                EnterprisePolicy(
                    type = PolicyType.CAMERA,
                    title = "Disabled Camera",
                    description = "Prevent users from accessing the device camera",
                    enabled = cameraDisabled
                )
            )
            _uiState.value = DashboardUiState(
                isAdminActive = deviceAdminManager.isAdminActive(),
                isDeviceOwner = deviceAdminManager.isDeviceOwner(),

                manufacture = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,

                isLoading = false,

                policyState = PolicyUiState(
                    isCameraDisabled = cameraDisabled
                ),

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

    fun setCameraDisabled(disabled: Boolean) {
        val success = enterprisePolicyManager.setCameraDisabled(disabled)

        if(success) {
            _uiState.update {
                it.copy(
                    policyState = it.policyState.copy(
                        isCameraDisabled = disabled
                    )
                )
            }
        }
    }
}