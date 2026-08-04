package com.floydwiz.googlemdm.presentation.components.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import com.floydwiz.googlemdm.presentation.components.common.PrimaryButton
import com.floydwiz.googlemdm.presentation.components.common.SectionTitle

@Composable
fun QuickActionCard(
    onActivateAdmin: () -> Unit,
    onRefresh: () -> Unit,
    isAdminActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            SectionTitle("Quick Actions")

            if(!isAdminActive) {
                PrimaryButton(
                    text = "Activate Device Admin",
                    onClick = onActivateAdmin
                )
            } else {
                Text("Device Administrator already Activated")
            }
            PrimaryButton(
                text = "Refresh",
                onClick = onRefresh
            )
        }
    }
}