package com.floydwiz.googlemdm.presentation.components.cards


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NetworkCard(
    isWifiEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column{
            Text(
                text = "Network",
                style = MaterialTheme.typography.titleMedium
            )
            Row {
                Text(text = "Wi-Fi")
                Spacer(
                    modifier = Modifier.width(8.dp))
                Text(
                    text = if (isWifiEnabled) {
                        "Enabled"
                    } else {
                        "Disabled"
                    }
                )
            }
        }
    }
}