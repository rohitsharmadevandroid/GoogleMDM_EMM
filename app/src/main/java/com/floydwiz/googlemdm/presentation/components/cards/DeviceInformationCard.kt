package com.floydwiz.googlemdm.presentation.components.cards


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.floydwiz.googlemdm.presentation.components.common.InfoRow
import com.floydwiz.googlemdm.presentation.components.common.SectionTitle

@Composable
fun DeviceInformationCard(
    manufacturer: String,
    model: String,
    androidVersion: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            SectionTitle("Device Information")
            InfoRow("Manufacturer", manufacturer)
            InfoRow("Model", model)
            InfoRow("Android Version", androidVersion)
        }
    }
}