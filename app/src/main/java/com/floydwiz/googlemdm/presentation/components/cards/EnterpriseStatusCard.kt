package com.floydwiz.googlemdm.presentation.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.floydwiz.googlemdm.presentation.components.common.SectionTitle
import com.floydwiz.googlemdm.presentation.components.status.StatusBadge

@Composable
fun EnterpriseStatusCard(
    isAdminActive: Boolean,
    isDeviceOwner: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            SectionTitle("Enterprise Status")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.material3.Text("Device Admin")
                StatusBadge(
                    enabled = isAdminActive
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.material3.Text("Device Owner")
                StatusBadge(
                    enabled = isDeviceOwner,
                    enabledText = "Owner",
                    disabledText = "Not Owner"
                )
            }
        }
    }
}