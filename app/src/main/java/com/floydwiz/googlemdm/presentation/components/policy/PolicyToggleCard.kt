package com.floydwiz.googlemdm.presentation.components.policy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.floydwiz.googlemdm.enterprise.policy.model.EnterprisePolicy

@Composable
fun PolicyToggleCard(
    policy: EnterprisePolicy,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = policy.title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = policy.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Switch(
            checked = policy.enabled,
            onCheckedChange = onToggle
        )
    }
}