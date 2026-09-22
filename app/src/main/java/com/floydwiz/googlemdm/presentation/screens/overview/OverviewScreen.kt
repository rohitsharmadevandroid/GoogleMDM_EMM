package com.floydwiz.googlemdm.presentation.screens.overview

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.floydwiz.googlemdm.enterprise.enrollment.model.EnrollmentState
import com.floydwiz.googlemdm.presentation.components.cards.DeviceInformationCard
import com.floydwiz.googlemdm.presentation.components.cards.QuickActionCard
import com.floydwiz.googlemdm.presentation.components.common.ScreenHeader
import com.floydwiz.googlemdm.presentation.components.common.StatTile
import com.floydwiz.googlemdm.presentation.theme.AccentBlue
import com.floydwiz.googlemdm.presentation.theme.Danger
import com.floydwiz.googlemdm.presentation.theme.Success
import com.floydwiz.googlemdm.presentation.theme.Warning
import com.floydwiz.googlemdm.presentation.viewmodel.DashboardViewModel

@Composable
fun OverviewScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.loadDeviceStatus()
    }

    val enrollmentLabel = when (uiState.enrollmentState) {
        EnrollmentState.NOT_ENROLLED -> "Not Enrolled"
        EnrollmentState.DEVICE_ADMIN -> "Device Admin"
        EnrollmentState.DEVICE_OWNER -> "Device Owner"
        EnrollmentState.ERROR -> "Error"
    }
    val enrollmentColor = when (uiState.enrollmentState) {
        EnrollmentState.NOT_ENROLLED -> Warning
        EnrollmentState.DEVICE_ADMIN, EnrollmentState.DEVICE_OWNER -> Success
        EnrollmentState.ERROR -> Danger
    }
    val policiesEnabledCount = uiState.policies.count { it.enabled }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Overview",
                subtitle = "This device's status at a glance"
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    label = "Device Admin",
                    value = if (uiState.isAdminActive) "Active" else "Inactive",
                    subText = if (uiState.isAdminActive) "Admin privileges granted" else "Not activated",
                    accentColor = if (uiState.isAdminActive) Success else Danger,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Device Owner",
                    value = if (uiState.isDeviceOwner) "Owner" else "Not Owner",
                    subText = if (uiState.isDeviceOwner) "Full management control" else "Limited control",
                    accentColor = if (uiState.isDeviceOwner) Success else Warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    label = "Enrollment",
                    value = enrollmentLabel,
                    subText = "Current enrollment state",
                    accentColor = enrollmentColor,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Policies Enabled",
                    value = "$policiesEnabledCount / ${uiState.policies.size}",
                    subText = "Enterprise policies active",
                    accentColor = AccentBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            DeviceInformationCard(
                manufacturer = uiState.manufacture,
                model = uiState.model,
                androidVersion = uiState.androidVersion
            )
        }
        item {
            QuickActionCard(
                isAdminActive = uiState.isAdminActive,
                onActivateAdmin = { launcher.launch(viewModel.createAdminIntent()) },
                onRefresh = { viewModel.loadDeviceStatus() }
            )
        }
    }
}
