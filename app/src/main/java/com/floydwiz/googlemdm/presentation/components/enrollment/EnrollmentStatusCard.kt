package com.floydwiz.googlemdm.presentation.components.enrollment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.floydwiz.googlemdm.enterprise.enrollment.model.EnrollmentState

@Composable
fun EnrollmentStatusCard(
    state: EnrollmentState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Enrollment",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = when (state) {
                    EnrollmentState.NOT_ENROLLED -> "Not Enrolled"
                    EnrollmentState.DEVICE_ADMIN -> "Device Admin"
                    EnrollmentState.DEVICE_OWNER -> "Device Owner"
                    EnrollmentState.ERROR -> "Error"
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}