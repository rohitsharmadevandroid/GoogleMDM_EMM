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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.floydwiz.googlemdm.BuildConfig
import com.floydwiz.googlemdm.presentation.navigation.Routes
import com.floydwiz.googlemdm.presentation.viewmodel.EmmViewModel
import com.floydwiz.googlemdm.presentation.viewmodel.EnrollmentViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.DateFormat
import java.util.Date

@Composable
fun EnrollmentScreen(
    navController: NavController,
    enrollmentViewModel: EnrollmentViewModel,
    emmViewModel: EmmViewModel = hiltViewModel()
) {
    val uiState by enrollmentViewModel.uiState.collectAsState()
    val emmUiState by emmViewModel.uiState.collectAsState()

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { emmViewModel.onQrCodeScanned(it) }
    }

    val context = LocalContext.current
    LaunchedEffect(emmUiState.justEnrolled) {
        if (emmUiState.justEnrolled) {
            Toast.makeText(
                context,
                "Enrolled successfully - device ID ${emmUiState.deviceId}",
                Toast.LENGTH_LONG
            ).show()
            emmViewModel.consumeJustEnrolledEvent()
        }
    }

    //Navigate to Dashboard after successful AMAPI Environment preparation
    LaunchedEffect(uiState.enrollmentCompleted) {
        if(uiState.enrollmentCompleted) {
            navController.navigate(Routes.DASHBOARD) {
                popUpTo(Routes.ENROLLMENT) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }
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

        // Device Status
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
        // AMAPI Environment
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
                        if(uiState.isEnvironmentPrepared) "Yes" else "No" 
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
            Text(text = "Prepare  AMAPI Environment")
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "EMM Backend",
            style = MaterialTheme.typography.headlineSmall
        )

        // EMM Backend Enrollment
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enrollment",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = "Enrolled: ${if (emmUiState.isEnrolled) "Yes" else "No"}")
                Text(text = "Device ID: ${emmUiState.deviceId ?: "-"}")

                if (emmUiState.isEnrolled) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { emmViewModel.resetEnrollment() }
                    ) {
                        Text(text = "Reset / Unenroll (clears local state only)")
                    }
                }

                OutlinedTextField(
                    value = emmUiState.enrollmentToken,
                    onValueChange = emmViewModel::onEnrollmentTokenChanged,
                    label = { Text("Enrollment Token") },
                    enabled = !emmUiState.isEnrolling,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !emmUiState.isEnrolling && !emmUiState.isEnrolled,
                    onClick = { emmViewModel.enroll() }
                ) {
                    Text(text = if (emmUiState.isEnrolling) "Enrolling..." else "Enroll")
                }

                Text(
                    text = "or",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !emmUiState.isEnrolling && !emmUiState.isEnrolled,
                    onClick = {
                        qrScanLauncher.launch(
                            ScanOptions()
                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                .setBeepEnabled(false)
                                .setOrientationLocked(false)
                                .setPrompt("Scan the enrollment QR code")
                        )
                    }
                ) {
                    Text(text = "Scan QR to Enroll")
                }

                emmUiState.enrollmentError?.let { error ->
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (BuildConfig.DEBUG) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { emmViewModel.installDevCaCertificate() }
                    ) {
                        Text(text = "Install Dev Backend CA Cert (Debug Only)")
                    }

                    emmUiState.devCaCertInstallResult?.let { message ->
                        Text(text = message)
                    }
                }
            }
        }

        // EMM Backend Check-In
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Check-In",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Last Successful Check-In: ${
                        emmUiState.lastCheckInAtMillis?.let {
                            DateFormat.getDateTimeInstance().format(Date(it))
                        } ?: "Never"
                    }"
                )
                Text(
                    text = "Server Polling Interval: ${
                        emmUiState.checkInIntervalSeconds?.let { "${it}s" } ?: "-"
                    }"
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !emmUiState.isCheckingIn && emmUiState.isEnrolled,
                    onClick = { emmViewModel.checkInNow() }
                ) {
                    Text(text = if (emmUiState.isCheckingIn) "Checking In..." else "Check In Now")
                }

                emmUiState.checkInError?.let { error ->
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}