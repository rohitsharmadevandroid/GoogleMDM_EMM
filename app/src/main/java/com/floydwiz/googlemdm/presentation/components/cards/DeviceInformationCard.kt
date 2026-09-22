package com.floydwiz.googlemdm.presentation.components.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.floydwiz.googlemdm.presentation.components.common.AppCard
import com.floydwiz.googlemdm.presentation.components.common.InfoRow
import com.floydwiz.googlemdm.presentation.components.common.SectionTitle

@Composable
fun DeviceInformationCard(
    manufacturer: String,
    model: String,
    androidVersion: String
) {
    AppCard {
        Column {
            SectionTitle("Device Information")
            InfoRow("Manufacturer", manufacturer)
            InfoRow("Model", model)
            InfoRow("Android Version", androidVersion)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
