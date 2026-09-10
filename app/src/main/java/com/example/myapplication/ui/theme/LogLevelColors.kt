package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.myapplication.data.model.LogLevel

fun getLogLevelColor(level: String): Color {
    return when (level) {
        LogLevel.DEBUG -> ColorDebug
        LogLevel.INFO -> ColorInfo
        LogLevel.WARNING -> ColorWarning
        LogLevel.ERROR -> ColorError
        LogLevel.CRITICAL -> ColorCritical
        else -> Color.Gray
    }
}
