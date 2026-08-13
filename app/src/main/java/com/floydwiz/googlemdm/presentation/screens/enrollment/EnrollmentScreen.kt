package com.floydwiz.googlemdm.presentation.screens.enrollment


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.floydwiz.googlemdm.presentation.viewmodel.EnrollmentViewModel

@Composable
fun EnrollmentScreen(
    enrollmentViewModel: EnrollmentViewModel = hiltViewModel(),
    onEnrollmentClick: () -> Unit
) {
    val uiState by enrollmentViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Enrollment",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Device Status",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Device Owner: ${
                        if (uiState.isDeviceOwner) "Active" else "Inactive"
                    }"
                )
                Text(
                    text = "Enrollment Status: ${uiState.enrollmentStatus}"
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AMAPI Environment",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Environment: ${
                        if(uiState.amapiEnvironmentAvailable) {
                            "AVAILABLE"
                        } else {
                            "UNAVAILABLE"
                        }
                    }"
                )
                Text(
                    text = "Android Device Policy State: ${uiState.androidDevicePolicyState}"
                )
                Text(
                    text = "Android Device Policy Version: ${uiState.androidDevicePolicyVersion}"
                )
                Text(
                    text = "Environment Prepared: ${ 
                        if(uiState.isPreparingEnvironment) "Yes" else "No" 
                    }"
                )
            }
        }

        //Loading
        if(
            uiState.isLoading ||
            uiState.isPreparingEnvironment
        ) {
            CircularProgressIndicator()
        }

        //Check Environment
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading &&
                    !uiState.isPreparingEnvironment,
            onClick = {
                enrollmentViewModel.loadEnrollmentState()
            }
        ) {
            Text(text = "Check Environment")
        }

        //Prepare Environment
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading &&
                    !uiState.isPreparingEnvironment,
            onClick = {
                enrollmentViewModel.prepareEnvironment()
            }
        ) {
            Text(text = "Prepare Environment")
        }

        //Error
        uiState.error?.let {
            error -> Card (
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}