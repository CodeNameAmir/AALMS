package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "categories")
@Serializable
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val userId: Long = 0,
    val color: String = "#3b82f6",
    val createdAt: Long = System.currentTimeMillis()
)
