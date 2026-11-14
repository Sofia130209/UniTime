package com.example.unitime.db.auth

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "avatar_${UUID.randomUUID()}.jpg" // * уникальное имя аватара
        val file = File(context.filesDir, fileName)

        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath // * абсолютный путь к сохранённому файлу
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}