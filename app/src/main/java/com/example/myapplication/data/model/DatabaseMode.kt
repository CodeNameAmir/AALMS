package com.example.myapplication.data.model

enum class DatabaseMode(
    val label: String,
    val description: String
) {
    LOCAL(
        label = "Local Database",
        description = "Stored on this device using local SQLite (Room)"
    ),
    GOOGLE_CLOUD(
        label = "Google Cloud (Firestore)",
        description = "Stored in Google Cloud Firestore with real-time sync"
    )
}
