package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String = "",
    val googleEmail: String? = null,
    val googleUid: String? = null,
    val googleDisplayName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

