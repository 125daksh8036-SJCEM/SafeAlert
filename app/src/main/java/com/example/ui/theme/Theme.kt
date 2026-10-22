package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EmergencyRed,
    onPrimary = Color.White,
    primaryContainer = EmergencyRedContainer,
    onPrimaryContainer = OnEmergencyRedContainer,
    secondary = SafetyGreen,
    onSecondary = Color.White,
    secondaryContainer = SafetyGreenContainer,
    onSecondaryContainer = OnSafetyGreenContainer,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = EmergencyRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = EmergencyRed,
    onPrimary = Color.White,
    primaryContainer = EmergencyRedDark,
    onPrimaryContainer = EmergencyRedContainer,
    secondary = SafetyGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF14532D),
    onSecondaryContainer = SafetyGreenContainer,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = TextDarkPrimary,
    surface = SurfaceDarkCard,
    onSurface = TextDarkPrimary,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = TextDarkSecondary,
    outline = SurfaceDarkBorder,
    error = EmergencyRed,
    onError = Color.White
)

@Composable
fun SafeAlertTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for backwards compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SafeAlertTheme(darkTheme = darkTheme, content = content)
}
