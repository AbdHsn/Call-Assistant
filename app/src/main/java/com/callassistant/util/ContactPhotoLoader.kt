package com.callassistant.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContactPhotoLoader {
    suspend fun loadBitmap(context: Context, photoUri: String): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            openPhotoStream(context, photoUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun openPhotoStream(context: Context, photoUri: String) = try {
        val uri = Uri.parse(photoUri)
        when (uri.scheme?.lowercase()) {
            "file" -> uri.path?.let { path -> File(path).inputStream() }
            else -> context.contentResolver.openInputStream(uri)
        }
    } catch (_: Exception) {
        null
    }
}
