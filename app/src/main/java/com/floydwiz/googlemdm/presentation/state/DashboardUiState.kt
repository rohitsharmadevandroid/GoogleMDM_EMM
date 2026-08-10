package com.floydwiz.googlemdm.presentation.state

import com.floydwiz.googlemdm.enterprise.network.model.NetworkState
import com.floydwiz.googlemdm.enterprise.policy.model.EnterprisePolicy

data class DashboardUiState(

    val error: String? = null,
    val isLoading: Boolean = false,

    val isAdminActive: Boolean = false,
    val isDeviceOwner: Boolean = false,

    val manufacture: String = "",
    val model: String = "",
    val androidVersion: String = "",

    val policies: List<EnterprisePolicy> = emptyList(),

    val isKioskModeEnabled: Boolean = false,

    val networkState: NetworkState = NetworkState(),
    val isWifiConfigDisabled: Boolean = false
)