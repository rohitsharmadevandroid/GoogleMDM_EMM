package com.floydwiz.googlemdm.presentation.screens.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.internal.enableLiveLiterals
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType
import com.floydwiz.googlemdm.presentation.components.cards.DeviceInformationCard
import com.floydwiz.googlemdm.presentation.components.cards.EnterpriseStatusCard
import com.floydwiz.googlemdm.presentation.components.cards.QuickActionCard
import com.floydwiz.googlemdm.presentation.components.policy.PolicyToggleCard
import com.floydwiz.googlemdm.presentation.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.loadDeviceStatus()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Google MDM",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Enterprise Device Controller",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            DeviceInformationCard(
                manufacturer = uiState.manufacture,
                model = uiState.model,
                androidVersion = uiState.androidVersion
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            EnterpriseStatusCard(
                isAdminActive = uiState.isAdminActive,
                isDeviceOwner = uiState.isDeviceOwner
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            QuickActionCard(
                isAdminActive = uiState.isAdminActive,
                onActivateAdmin = {
                    Logger.d("DashboardScreen: Activating Admin")
                    launcher.launch(
                        viewModel.createAdminIntent()
                    )
                },
                onRefresh = { viewModel.loadDeviceStatus() }
            )
        }
        items(uiState.policies) { policy ->
            PolicyToggleCard(
                policy = policy,
                onToggle = { enabled ->
                    viewModel.setPolicy(
                        policy.type,
                        enabled
                    )
                }
            )
        }
    }
}