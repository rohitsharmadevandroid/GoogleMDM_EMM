package com.floydwiz.googlemdm.presentation.screens.policies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.floydwiz.googlemdm.enterprise.kiosk.KioskController
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType
import com.floydwiz.googlemdm.presentation.components.common.ScreenHeader
import com.floydwiz.googlemdm.presentation.components.policy.PolicyToggleCard
import com.floydwiz.googlemdm.presentation.viewmodel.DashboardViewModel

@Composable
fun PoliciesScreen(
    kioskController: KioskController,
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
                title = "Policies",
                subtitle = "Enterprise policies enforced on this device"
            )
        }
        items(uiState.policies) { policy ->
            PolicyToggleCard(
                policy = policy,
                onToggle = { enabled ->
                    if (policy.type == PolicyType.KIOSK) {
                        if (enabled) {
                            viewModel.enabledKioskMode()
                            kioskController.startKiosk()
                        } else {
                            kioskController.stopKiosk()
                            viewModel.disabledKioskMode()
                        }
                    } else {
                        viewModel.setPolicy(policy.type, enabled)
                    }
                }
            )
        }
    }
}
