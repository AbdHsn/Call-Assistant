package com.callassistant.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
internal fun PhotoCropDialog(
    sourceUri: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val boxSize = 280.dp
    val boxPx = with(LocalDensity.current) { boxSize.toPx() }

    LaunchedEffect(sourceUri) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (_: Exception) {
                null
            }
        }
        loadFailed = bitmap == null
    }

    fun clampOffset(bmp: Bitmap, s: Float, o: Offset): Offset {
        val base = maxOf(boxPx / bmp.width.toFloat(), boxPx / bmp.height.toFloat())
        val maxX = maxOf(0f, (bmp.width * base * s - boxPx) / 2f)
        val maxY = maxOf(0f, (bmp.height * base * s - boxPx) / 2f)
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    fun rotate(degrees: Float) {
        val src = bitmap ?: return
        val matrix = Matrix().apply { postRotate(degrees) }
        bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        scale = 1f
        offset = Offset.Zero
    }

    fun cropAndSave(bmp: Bitmap): String? {
        val base = maxOf(boxPx / bmp.width.toFloat(), boxPx / bmp.height.toFloat())
        val s = base * scale
        val topLeftX = boxPx / 2f + offset.x - bmp.width * s / 2f
        val topLeftY = boxPx / 2f + offset.y - bmp.height * s / 2f
        val left = (-topLeftX / s).coerceIn(0f, bmp.width - 1f)
        val top = (-topLeftY / s).coerceIn(0f, bmp.height - 1f)
        val size = minOf(boxPx / s, bmp.width - left, bmp.height - top)
        if (size <= 0f) return null
        val cropped = Bitmap.createBitmap(
            bmp, left.roundToInt(), top.roundToInt(), size.roundToInt(), size.roundToInt()
        )
        val out = if (cropped.width > 512) Bitmap.createScaledBitmap(cropped, 512, 512, true) else cropped
        val dir = File(context.filesDir, "contact_photos").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        return try {
            FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            ).toString()
        } catch (_: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust photo") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(boxSize)
                        .clipToBounds()
                        .background(Color.Black)
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val bmp = bitmap ?: return@detectTransformGestures
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                offset = clampOffset(bmp, newScale, offset + pan)
                                scale = newScale
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmap
                    when {
                        bmp != null -> {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Photo to crop",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    },
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, Color.White)
                            )
                        }
                        loadFailed -> Text(
                            "Couldn't load image",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        else -> CircularProgressIndicator()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { rotate(-90f) }, enabled = bitmap != null) {
                        Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate left")
                    }
                    IconButton(onClick = { rotate(90f) }, enabled = bitmap != null) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate right")
                    }
                }
                Text(
                    "Pinch to zoom, drag to move",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bmp = bitmap ?: return@TextButton
                    scope.launch {
                        val uri = withContext(Dispatchers.IO) { cropAndSave(bmp) }
                        if (uri != null) onConfirm(uri)
                    }
                },
                enabled = bitmap != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
