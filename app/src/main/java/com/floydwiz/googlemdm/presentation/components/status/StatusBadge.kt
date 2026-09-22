package com.floydwiz.googlemdm.presentation.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.floydwiz.googlemdm.presentation.theme.Danger
import com.floydwiz.googlemdm.presentation.theme.Success

@Composable
fun StatusBadge(
    enabled: Boolean,
    enabledText: String = "Enabled",
    disabledText: String = "Disabled",
) {
    val color = if (enabled) Success else Danger
    Text(
        text = if (enabled) enabledText else disabledText,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun Pill(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
