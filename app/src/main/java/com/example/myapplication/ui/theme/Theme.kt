package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF022C22),
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = EmeraldPrimaryLight,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF064E3B).copy(alpha = 0.6f),
    onSecondaryContainer = EmeraldOnPrimaryContainer,
    tertiary = Color(0xFF14B8A6),
    onTertiary = Color(0xFF042F2C),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = DarkBg,
    onBackground = TextPrimaryDark,
    surface = DarkBg2,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkBg3,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = DarkBg2,
    surfaceContainerHigh = DarkBg3,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
    error = ColorError,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF3F1D24),
    onErrorContainer = Color(0xFFFECDD3)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6FFFA),
    onTertiaryContainer = Color(0xFF134E4A),
    background = LightCanvas,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceVariant,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightBorderRefined,
    outlineVariant = LightBorderSubtleRefined,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamicColor by default to prevent device wallpaper blue tint from overriding custom palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
