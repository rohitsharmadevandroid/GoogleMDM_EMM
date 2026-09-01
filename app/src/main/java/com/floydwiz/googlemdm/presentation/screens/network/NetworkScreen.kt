package com.floydwiz.googlemdm.presentation.screens.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.floydwiz.googlemdm.enterprise.network.model.NetworkPolicyType
import com.floydwiz.googlemdm.presentation.components.common.ScreenHeader
import com.floydwiz.googlemdm.presentation.components.network.NetworkToggleCard
import com.floydwiz.googlemdm.presentation.viewmodel.DashboardViewModel

@Composable
fun NetworkScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Network",
                subtitle = "Manage this device's network policies"
            )
        }
        item {
            NetworkToggleCard(
                title = "Disable Wi-Fi Configuration",
                description = "Prevent users from changing Wi-Fi configuration",
                enabled = uiState.isWifiConfigDisabled,
                onToggle = { disabled ->
                    viewModel.setNetworkPolicy(NetworkPolicyType.WIFI, disabled)
                }
            )
        }
    }
}
