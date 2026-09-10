package com.example.myapplication.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun saveFileToInternal(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val fileName = "attach_${System.currentTimeMillis()}"
            val file = File(context.filesDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
