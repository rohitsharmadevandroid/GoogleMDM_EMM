package com.floydwiz.googlemdm.presentation.components.status

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun StatusBadge(
    enabled: Boolean,
    enabledText: String = "Enabled",
    disabledText: String = "Disabled",
) {
    Text(
        text = if (enabled) "Enabled" else "Disabled",
        style = MaterialTheme.typography.bodyMedium,
    )
}