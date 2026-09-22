package com.floydwiz.googlemdm.presentation.viewmodel

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.enrollment.handler.EnrollmentHandler
import com.floydwiz.googlemdm.enterprise.enrollment.manager.AmapiEnvironmentManager
import com.floydwiz.googlemdm.enterprise.kiosk.manager.KioskManager
import com.floydwiz.googlemdm.enterprise.network.handler.NetworkPolicyHandler
import com.floydwiz.googlemdm.enterprise.network.manager.NetworkManager
import com.floydwiz.googlemdm.enterprise.network.model.NetworkPolicyType
import com.floydwiz.googlemdm.enterprise.network.model.NetworkState
import com.floydwiz.googlemdm.enterprise.policy.handler.PolicyHandler
import com.floydwiz.googlemdm.enterprise.policy.manager.EnterprisePolicyManager
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType
import com.floydwiz.googlemdm.enterprise.policy.registry.PolicyDefinitions
import com.floydwiz.googlemdm.presentation.state.DashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager,
    //private val enterprisePolicyManager: EnterprisePolicyManager,
    private val kioskManager: KioskManager,
    private val policyHandler: PolicyHandler,
    private val networkPolicyHandler: NetworkPolicyHandler,
    private val enrollmentHandler: EnrollmentHandler,
    private val amapiEnvironmentManager: AmapiEnvironmentManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDeviceStatus()

        prepareAmapiEnvironment()
    }

    private fun updatePolicyState(
        type: PolicyType,
        disabled: Boolean
    ){
        _uiState.update { state ->
            state.copy(
                policies = state.policies.map { policy ->
                    if (policy.type == type) {
                        policy.copy(enabled = disabled)
                    }
                    else {
                        policy
                    }
                }
            )
        }
    }

    fun loadDeviceStatus() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val kioskEnabled = kioskManager.isKioskModeEnabled()
            val networkState = NetworkState(
                isWifiEnabled = networkPolicyHandler.getNetworkState()
            )
            Logger.d("Dashboard Wi-Fi state = ${networkState.isWifiEnabled}")
            val wifiConfigDisabled = networkPolicyHandler.getPolicy(NetworkPolicyType.WIFI)
            Logger.d("Dashboard Wi-Fi config disabled = $wifiConfigDisabled")

            val policies = PolicyDefinitions.all
                .map { definition ->
                    PolicyDefinitions.createPolicy(
                        definition = definition,
                        enabled = policyHandler.getPolicy(definition.type)
                    )
                }.toMutableList()

            val enrollmentState = enrollmentHandler.getEnrollmentState()
            Logger.d("Dashboard Enrollment State = $enrollmentState")

            policies.add(
                PolicyDefinitions.createPolicy(
                    definition = PolicyDefinitions.KIOSK,
                    enabled = kioskEnabled
                )
            )
            _uiState.value = DashboardUiState(
                isAdminActive = deviceAdminManager.isAdminActive(),
                isDeviceOwner = deviceAdminManager.isDeviceOwner(),

                manufacture = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,

                isLoading = false,

                policies = policies,
                networkState = networkState,
                isWifiConfigDisabled = wifiConfigDisabled,

                enrollmentState = enrollmentState
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
        }
    }

    fun createAdminIntent(): Intent {
        Logger.d("DashboardViewModel createAdminIntent: Creating admin intent")
        return deviceAdminManager.createAdminIntent()
    }

    fun setPolicy(
        type: PolicyType,
        enabled: Boolean
    ) {
        if (type == PolicyType.KIOSK)
        {
            if(enabled) {
                enabledKioskMode()
            } else {
                disabledKioskMode()
            }
            return
        }

        if(policyHandler.setPolicy(type, enabled))
        {
            updatePolicyState(type, enabled)
        }
    }

    //Kiosk Mode
    fun enabledKioskMode() {
        if(kioskManager.enabledKioskMode( )) {
            updatePolicyState(PolicyType.KIOSK, true)
        }
    }

    fun disabledKioskMode() {
        if(kioskManager.disabledKioskMode()) {
            updatePolicyState(PolicyType.KIOSK, false)
        }
    }

    fun setNetworkPolicy(
        type: NetworkPolicyType,
        disabled: Boolean
    ) {
        if(networkPolicyHandler.setPolicy(type, disabled)) {
            loadDeviceStatus()
        }
    }

    //Enrollment AMAPI Function
    fun prepareAmapiEnvironment() {
        viewModelScope.launch {

            Logger.d("Starting AMAPI Environment preparation")

            val response =
                amapiEnvironmentManager.prepareEnvironment()

            if (response != null) {

                Logger.d(
                    "AMAPI Prepare Environment Status = ${response.environment}"
                )

                val environment =
                    amapiEnvironmentManager.getEnvironment()

                if (environment != null) {
                    Logger.d(
                        "AMAPI Environment check successfully"
                    )
                } else {
                    Logger.e(
                        "AMAPI Environment check failed after preparation"
                    )
                }

            } else {

                Logger.e(
                    "AMAPI Environment preparation failed"
                )
            }
        }
    }
}