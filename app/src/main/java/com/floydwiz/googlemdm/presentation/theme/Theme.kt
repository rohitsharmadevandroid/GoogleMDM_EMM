package com.floydwiz.googlemdm.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val McmDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = Color.White,
    tertiary = Purple,
    onTertiary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = SecondarySurface,
    onSurfaceVariant = TextBodyMuted,
    outline = Border,
    outlineVariant = BorderHover,
    error = Danger,
    onError = Color.White,
    errorContainer = Danger.copy(alpha = 0.12f),
    onErrorContainer = DangerLight,
)

@Composable
fun Google_MdmTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = McmDarkColorScheme,
        typography = Typography,
        content = content
    )
}
