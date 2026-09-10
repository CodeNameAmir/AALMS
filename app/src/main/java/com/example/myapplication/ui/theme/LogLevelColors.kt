package com.example.myapplication.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.myapplication.data.model.LogLevel

fun getLogLevelColor(level: String, isDark: Boolean = true): Color {
    return if (isDark) {
        when (level) {
            LogLevel.DEBUG -> ColorDebug
            LogLevel.INFO -> ColorInfo
            LogLevel.WARNING -> ColorWarning
            LogLevel.ERROR -> ColorError
            LogLevel.CRITICAL -> ColorCritical
            else -> Color(0xFF94A3B8)
        }
    } else {
        when (level) {
            LogLevel.DEBUG -> Color(0xFF475569) // Slate 600
            LogLevel.INFO -> Color(0xFF0284C7)  // Sky 600
            LogLevel.WARNING -> Color(0xFFD97706) // Amber 600
            LogLevel.ERROR -> Color(0xFFDC2626)   // Red 600
            LogLevel.CRITICAL -> Color(0xFFE11D48) // Crimson / Rose 600
            else -> Color(0xFF64748B)
        }
    }
}

@Composable
fun rememberLogLevelColor(level: String): Color {
    val isDark = isSystemInDarkTheme()
    return getLogLevelColor(level, isDark)
}

