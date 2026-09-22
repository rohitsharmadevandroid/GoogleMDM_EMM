package com.floydwiz.googlemdm.presentation.components.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.floydwiz.googlemdm.presentation.components.common.AppCard
import com.floydwiz.googlemdm.presentation.components.common.PrimaryButton
import com.floydwiz.googlemdm.presentation.components.common.SectionTitle

@Composable
fun QuickActionCard(
    onActivateAdmin: () -> Unit,
    onRefresh: () -> Unit,
    isAdminActive: Boolean
) {
    AppCard {
        Column {
            SectionTitle("Quick Actions")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (!isAdminActive) {
                    PrimaryButton(
                        text = "Activate Device Admin",
                        onClick = onActivateAdmin
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Text(
                        text = "Device Administrator already activated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                PrimaryButton(
                    text = "Refresh",
                    onClick = onRefresh
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
