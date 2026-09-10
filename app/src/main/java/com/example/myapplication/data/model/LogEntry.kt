package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "logs",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("categoryId"),
        Index("pinned", "id")
    ]
)
@Serializable
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val level: String, // DEBUG, INFO, WARNING, ERROR, CRITICAL
    val categoryId: Long,
    val userId: Long,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val attachmentName: String? = null,
    val attachmentPath: String? = null,
    val attachmentSize: Long? = null
)
