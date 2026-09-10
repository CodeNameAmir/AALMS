package com.example.myapplication.data.model

object LogLevel {
    const val DEBUG = "DEBUG"
    const val INFO = "INFO"
    const val WARNING = "WARNING"
    const val ERROR = "ERROR"
    const val CRITICAL = "CRITICAL"

    val all = listOf(DEBUG, INFO, WARNING, ERROR, CRITICAL)
}
