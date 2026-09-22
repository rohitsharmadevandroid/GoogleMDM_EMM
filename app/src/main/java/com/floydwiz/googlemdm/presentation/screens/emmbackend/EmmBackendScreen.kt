package com.floydwiz.googlemdm.presentation.screens.emmbackend

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.floydwiz.googlemdm.BuildConfig
import com.floydwiz.googlemdm.presentation.components.common.AppCard
import com.floydwiz.googlemdm.presentation.components.common.PrimaryButton
import com.floydwiz.googlemdm.presentation.components.common.ScreenHeader
import com.floydwiz.googlemdm.presentation.components.common.SectionTitle
import com.floydwiz.googlemdm.presentation.components.status.Pill
import com.floydwiz.googlemdm.presentation.theme.Danger
import com.floydwiz.googlemdm.presentation.theme.Success
import com.floydwiz.googlemdm.presentation.viewmodel.EmmViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.DateFormat
import java.util.Date

@Composable
fun EmmBackendScreen(
    viewModel: EmmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.onQrCodeScanned(it) }
    }

    LaunchedEffect(uiState.justEnrolled) {
        if (uiState.justEnrolled) {
            Toast.makeText(
                context,
                "Enrolled successfully - device ID ${uiState.deviceId}",
                Toast.LENGTH_LONG
            ).show()
            viewModel.consumeJustEnrolledEvent()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "EMM Backend",
                subtitle = "Enroll and sync this device with your MDM backend"
            )
        }

        item {
            AppCard {
                Column {
                    SectionTitle("Enrollment")
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Pill(
                            text = if (uiState.isEnrolled) "Enrolled" else "Not Enrolled",
                            color = if (uiState.isEnrolled) Success else Danger
                        )
                        Text(
                            text = "Device ID: ${uiState.deviceId ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        if (uiState.isEnrolled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PrimaryButton(
                                text = "Reset / Unenroll (clears local state only)",
                                onClick = { viewModel.resetEnrollment() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.enrollmentToken,
                            onValueChange = viewModel::onEnrollmentTokenChanged,
                            label = { Text("Enrollment Token") },
                            enabled = !uiState.isEnrolling,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        PrimaryButton(
                            text = if (uiState.isEnrolling) "Enrolling..." else "Enroll",
                            enabled = !uiState.isEnrolling && !uiState.isEnrolled,
                            onClick = { viewModel.enroll() }
                        )

                        Text(
                            text = "or",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isEnrolling && !uiState.isEnrolled,
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
                            Text("Scan QR to Enroll")
                        }

                        uiState.enrollmentError?.let { error ->
                            Text(
                                text = "Error: $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { viewModel.installDevCaCertificate() }
                            ) {
                                Text("Install Dev Backend CA Cert (Debug Only)")
                            }
                            uiState.devCaCertInstallResult?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        item {
            AppCard {
                Column {
                    SectionTitle("Check-In")
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Last Successful Check-In: ${
                                uiState.lastCheckInAtMillis?.let {
                                    DateFormat.getDateTimeInstance().format(Date(it))
                                } ?: "Never"
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Server Polling Interval: ${
                                uiState.checkInIntervalSeconds?.let { "${it}s" } ?: "-"
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(
                            text = if (uiState.isCheckingIn) "Checking In..." else "Check In Now",
                            enabled = !uiState.isCheckingIn && uiState.isEnrolled,
                            onClick = { viewModel.checkInNow() }
                        )

                        uiState.checkInError?.let { error ->
                            Text(
                                text = "Error: $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
