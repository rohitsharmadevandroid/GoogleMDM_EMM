package com.floydwiz.googlemdm.presentation.screens.dashboard

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import com.floydwiz.googlemdm.enterprise.kiosk.KioskController
import com.floydwiz.googlemdm.enterprise.network.model.NetworkPolicyType
import com.floydwiz.googlemdm.enterprise.policy.model.EnterprisePolicy
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType
import com.floydwiz.googlemdm.presentation.components.cards.DeviceInformationCard
import com.floydwiz.googlemdm.presentation.components.cards.EnterpriseStatusCard
import com.floydwiz.googlemdm.presentation.components.cards.NetworkCard
import com.floydwiz.googlemdm.presentation.components.cards.QuickActionCard
import com.floydwiz.googlemdm.presentation.components.network.NetworkToggleCard
import com.floydwiz.googlemdm.presentation.components.policy.PolicyToggleCard
import com.floydwiz.googlemdm.presentation.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    kioskController: KioskController,
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
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Network",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            NetworkToggleCard(
                title = "Disable Wi-Fi Configuration",
                description = "Prevent users from changing Wi-Fi configuration",
                enabled = uiState.isWifiConfigDisabled,
                onToggle = { disabled ->
                    viewModel.setNetworkPolicy(
                        NetworkPolicyType.WIFI,
                        disabled
                    )
                }
            )
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
                    if(policy.type == PolicyType.KIOSK) {
                       if (enabled) {
                           viewModel.enabledKioskMode()
                           kioskController.startKiosk()
                       } else {
                           kioskController.stopKiosk()
                           viewModel.disabledKioskMode()
                       }
                    }   else {
                        viewModel.setPolicy(
                            policy.type,
                            enabled
                        )
                    }
                }
            )
        }
    }
}