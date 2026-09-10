package com.example.myapplication.util

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.LogWithCategory
import com.example.myapplication.data.model.ExternalLogEntry
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object ImportExportUtils {

    val gson = GsonBuilder().setPrettyPrinting().create()

    fun exportToJson(context: Context, uri: Uri, logs: List<LogWithCategory>) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    val externalLogs = logs.map { it.toExternal() }
                    gson.toJson(externalLogs, writer)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToCsv(context: Context, uri: Uri, logs: List<LogWithCategory>) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("id,level,category,pinned,message,attachment,attachment_size,created_at,updated_at\n")
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    logs.forEach { l ->
                        val log = l.log
                        val createdAtStr = sdf.format(Date(log.createdAt))
                        val updatedAtStr = sdf.format(Date(log.updatedAt))
                        val csvLine = "${log.id},${log.level},\"${l.categoryName.replace("\"", "\"\"")}\",${log.pinned.toString().uppercase()},\"${log.message.replace("\"", "\"\"")}\",\"${(log.attachmentName ?: "").replace("\"", "\"\"")}\",${log.attachmentSize ?: 0},$createdAtStr,$updatedAtStr\n"
                        writer.write(csvLine)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importFromJson(context: Context, uri: Uri): List<ExternalLogEntry>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = inputStream.bufferedReader()
                val type = object : TypeToken<List<ExternalLogEntry>>() {}.type
                gson.fromJson(reader, type)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun importFromCsv(context: Context, uri: Uri): List<ExternalLogEntry>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                importFromCsvContent(content)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun importFromCsvContent(content: String): List<ExternalLogEntry>? {
        return try {
            val logs = mutableListOf<ExternalLogEntry>()
            val lines = content.lines()
            if (lines.size > 1) {
                val header = lines[0].lowercase()
                val partsHeader = parseCsvLine(header)
                
                val idxId = partsHeader.indexOf("id")
                val idxLevel = partsHeader.indexOf("level")
                val idxCategory = partsHeader.indexOf("category")
                val idxPinned = partsHeader.indexOf("pinned")
                val idxMessage = partsHeader.indexOf("message")
                val idxAttachment = partsHeader.indexOf("attachment")
                val idxAttachmentSize = partsHeader.indexOf("attachment_size")
                val idxCreatedAt = partsHeader.indexOf("created_at")
                val idxUpdatedAt = partsHeader.indexOf("updated_at")

                for (i in 1 until lines.size) {
                    val line = lines[i]
                    if (line.isBlank()) continue
                    val parts = parseCsvLine(line)
                    
                    logs.add(ExternalLogEntry(
                        id = if (idxId != -1) parts.getOrNull(idxId)?.toLongOrNull() else null,
                        level = if (idxLevel != -1) parts.getOrNull(idxLevel) ?: "INFO" else "INFO",
                        categoryName = if (idxCategory != -1) parts.getOrNull(idxCategory) else null,
                        pinned = if (idxPinned != -1) parts.getOrNull(idxPinned)?.equals("TRUE", ignoreCase = true) ?: false else false,
                        message = if (idxMessage != -1) parts.getOrNull(idxMessage) ?: "" else "",
                        attachmentName = if (idxAttachment != -1) parts.getOrNull(idxAttachment) else null,
                        attachmentSize = if (idxAttachmentSize != -1) parts.getOrNull(idxAttachmentSize)?.toLongOrNull() else null,
                        createdAt = if (idxCreatedAt != -1) parts.getOrNull(idxCreatedAt) else null,
                        updatedAt = if (idxUpdatedAt != -1) parts.getOrNull(idxUpdatedAt) else null
                    ))
                }
            }
            logs
        } catch (e: Exception) {
            null
        }
    }

    internal fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentPart = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && (i + 1 < line.length) && line[i + 1] == '\"') {
                    currentPart.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentPart.toString())
                currentPart = StringBuilder()
            } else {
                currentPart.append(c)
            }
            i++
        }
        result.add(currentPart.toString())
        return result
    }

    fun parseTimestamp(timestamp: String?): Long {
        if (timestamp == null) return System.currentTimeMillis()
        
        // Try as Long
        timestamp.toLongOrNull()?.let { return it }

        // Normalize fractional seconds if present (e.g. .0799478Z -> .079Z)
        val normalized = if (timestamp.contains(".") && timestamp.endsWith("Z")) {
            val dotIdx = timestamp.indexOf('.')
            val zIdx = timestamp.lastIndexOf('Z')
            val fraction = timestamp.substring(dotIdx + 1, zIdx)
            val millisFraction = if (fraction.length > 3) fraction.take(3) else fraction.padEnd(3, '0')
            timestamp.substring(0, dotIdx + 1) + millisFraction + "Z"
        } else {
            timestamp
        }
        
        // Try ISO formats
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                if (format.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(normalized)?.time ?: continue
            } catch (e: Exception) {
                // ignore
            }
        }
        
        return System.currentTimeMillis()
    }

    internal fun LogWithCategory.toExternal(): ExternalLogEntry {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'0000Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return ExternalLogEntry(
            id = log.id,
            userId = log.userId.toString(),
            message = log.message,
            level = log.level,
            categoryId = log.categoryId,
            categoryName = categoryName,
            categoryColor = categoryColor,
            pinned = log.pinned,
            createdAt = sdf.format(Date(log.createdAt)),
            updatedAt = sdf.format(Date(log.updatedAt)),
            attachmentName = log.attachmentName,
            attachmentPath = log.attachmentPath?.substringAfterLast("/"),
            attachmentSize = log.attachmentSize
        )
    }
}
