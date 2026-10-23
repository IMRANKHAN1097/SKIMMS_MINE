package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkimmsDarkColorScheme = darkColorScheme(
    primary = SafetyAmber,
    onPrimary = Color(0xFF1A1200),
    primaryContainer = Color(0xFF332400),
    onPrimaryContainer = SafetyAmberLight,
    secondary = SensorCyan,
    onSecondary = Color(0xFF002229),
    secondaryContainer = Color(0xFF003844),
    onSecondaryContainer = SensorCyan,
    tertiary = StatusCaution,
    onTertiary = Color.Black,
    background = MineDarkBackground,
    onBackground = TextPrimary,
    surface = MineDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = MineDarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = StatusCritical,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SkimmsDarkColorScheme,
        typography = Typography,
        content = content
    )
}
