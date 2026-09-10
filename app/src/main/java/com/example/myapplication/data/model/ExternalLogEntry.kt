package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExternalLogEntry(
    @SerializedName("id") @SerialName("id") val id: Long? = null,
    @SerializedName("user_id") @SerialName("user_id") val userId: String? = null,
    @SerializedName("message") @SerialName("message") val message: String,
    @SerializedName("level") @SerialName("level") val level: String,
    @SerializedName("category_id") @SerialName("category_id") val categoryId: Long? = null,
    @SerializedName("category_name") @SerialName("category_name") val categoryName: String? = null,
    @SerializedName("category_color") @SerialName("category_color") val categoryColor: String? = null,
    @SerializedName("pinned") @SerialName("pinned") val pinned: Boolean = false,
    @SerializedName("created_at") @SerialName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") @SerialName("updated_at") val updatedAt: String? = null,
    @SerializedName("attachment_name") @SerialName("attachment_name") val attachmentName: String? = null,
    @SerializedName("attachment_path") @SerialName("attachment_path") val attachmentPath: String? = null,
    @SerializedName("attachment_size") @SerialName("attachment_size") val attachmentSize: Long? = null
)
