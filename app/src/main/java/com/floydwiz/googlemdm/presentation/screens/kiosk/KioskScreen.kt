package com.floydwiz.googlemdm.presentation.screens.kiosk

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class DummyApp(
    val label: String,
    val emoji: String,
    val color: Color
)

private val dummyApps = listOf(
    DummyApp("Browser", "🌐", Color(0xFF3B82F6)),
    DummyApp("Camera", "📷", Color(0xFF22C55E)),
    DummyApp("Calculator", "🧮", Color(0xFFF59E0B)),
    DummyApp("Notes", "📝", Color(0xFF8B5CF6)),
    DummyApp("Gallery", "🖼️", Color(0xFF06B6D4)),
    DummyApp("Settings", "⚙️", Color(0xFF64748B)),
)

/**
 * Shown instead of the normal MDM UI whenever kiosk mode is active - a
 * device locked into this app via lock task shouldn't be showing an admin
 * management screen. Every "app" here is a non-functional placeholder;
 * a real kiosk deployment would launch actual allow-listed apps instead.
 */
@Composable
fun KioskScreen(
    onExitKiosk: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Kiosk Mode",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "This device is locked to the apps below.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(dummyApps) { app ->
                DummyAppTile(app) {
                    Toast.makeText(
                        context,
                        "${app.label} is a demo app - not functional in kiosk mode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        TextButton(onClick = onExitKiosk) {
            Text(text = "Exit Kiosk Mode (Admin)")
        }
    }
}

@Composable
private fun DummyAppTile(
    app: DummyApp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(app.color, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = app.emoji, fontSize = 28.sp)
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
