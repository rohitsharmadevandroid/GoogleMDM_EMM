package com.floydwiz.googlemdm.presentation.components.policy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.floydwiz.googlemdm.enterprise.policy.model.EnterprisePolicy
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType
import com.floydwiz.googlemdm.presentation.components.common.AppCard
import com.floydwiz.googlemdm.presentation.components.status.Pill
import com.floydwiz.googlemdm.presentation.theme.AccentBlue
import com.floydwiz.googlemdm.presentation.theme.Cyan
import com.floydwiz.googlemdm.presentation.theme.Danger
import com.floydwiz.googlemdm.presentation.theme.Purple
import com.floydwiz.googlemdm.presentation.theme.Warning

private fun categoryFor(type: PolicyType): Pair<String, Color> = when (type) {
    PolicyType.CAMERA,
    PolicyType.SCREEN_CAPTURE,
    PolicyType.USB_FILE_TRANSFER,
    PolicyType.SAFE_BOOT,
    PolicyType.FACTORY_RESET -> "Security" to Danger
    PolicyType.ADD_USER -> "Access Control" to AccentBlue
    PolicyType.OUTGOING_CALLS,
    PolicyType.SMS -> "Communication" to Cyan
    PolicyType.KIOSK -> "Configuration" to Warning
    PolicyType.WIFI -> "Network" to Purple
}

@Composable
fun PolicyToggleCard(
    policy: EnterprisePolicy,
    onToggle: (Boolean) -> Unit,
) {
    val (categoryLabel, categoryColor) = categoryFor(policy.type)
    AppCard {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Pill(text = categoryLabel, color = categoryColor)
                Text(
                    text = policy.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = policy.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Switch(
                checked = policy.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentBlue,
                    checkedTrackColor = AccentBlue.copy(alpha = 0.35f)
                )
            )
        }
    }
}
