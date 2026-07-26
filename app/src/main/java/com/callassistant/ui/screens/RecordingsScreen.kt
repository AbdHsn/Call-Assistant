package com.callassistant.ui.screens

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.callassistant.util.CallRecorder
import com.callassistant.util.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var recordings by remember { mutableStateOf(listOf<Recording>()) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        recordings = withContext(Dispatchers.IO) { CallRecorder.listRecordings(context) }
    }

    var activePath by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }

    fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        activePath = null
        isPlaying = false
        positionMs = 0
        durationMs = 0
    }

    fun playRecording(recording: Recording) {
        val path = recording.file.absolutePath
        releasePlayer()

        // A prior phone call can leave audio routing stuck in call mode (routed to the
        // earpiece at a very low volume), which makes normal media playback inaudible.
        // Reset routing back to normal media playback before starting.
        val audioManager = context.getSystemService(AudioManager::class.java)
        audioManager?.let {
            it.mode = AudioManager.MODE_NORMAL
            it.isSpeakerphoneOn = true
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(1f, 1f)
                setDataSource(path)
                setOnPreparedListener {
                    durationMs = it.duration
                    it.start()
                    isPlaying = true
                }
                setOnCompletionListener {
                    isPlaying = false
                    positionMs = 0
                    it.seekTo(0)
                }
                prepareAsync()
            }
            mediaPlayer = player
            activePath = path
        } catch (e: Exception) {
            releasePlayer()
        }
    }

    fun togglePlayPause(recording: Recording) {
        val path = recording.file.absolutePath
        if (activePath == path && mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer?.pause()
                isPlaying = false
            } else {
                mediaPlayer?.start()
                isPlaying = true
            }
        } else {
            playRecording(recording)
        }
    }

    fun seekTo(ms: Int) {
        mediaPlayer?.seekTo(ms)
        positionMs = ms
    }

    LaunchedEffect(activePath, isPlaying) {
        while (isPlaying) {
            positionMs = mediaPlayer?.currentPosition ?: 0
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (recordings.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "No call recordings yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordings, key = { it.file.absolutePath }) { recording ->
                    val isActive = activePath == recording.file.absolutePath
                    RecordingItem(
                        recording = recording,
                        isActive = isActive,
                        isPlaying = isActive && isPlaying,
                        positionMs = if (isActive) positionMs else 0,
                        durationMs = if (isActive) durationMs else 0,
                        onPlayPause = { togglePlayPause(recording) },
                        onSeek = { seekTo(it) },
                        onShare = {
                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                recording.file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/mp4"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share recording"))
                        },
                        onDelete = {
                            if (isActive) releasePlayer()
                            recording.file.delete()
                            refreshKey++
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingItem(
    recording: Recording,
    isActive: Boolean,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var durationLabel by remember(recording.file.absolutePath) { mutableStateOf("--:--") }
    LaunchedEffect(recording.file.absolutePath) {
        durationLabel = withContext(Dispatchers.IO) { extractDuration(recording.file.absolutePath) }
    }
    var dragPositionMs by remember { mutableStateOf<Float?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = recording.number, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (isActive) {
                            "${formatMs(dragPositionMs?.toInt() ?: positionMs)} / ${formatMs(durationMs)}"
                        } else {
                            "${formatTimestamp(recording.timestamp)} \u2022 $durationLabel"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
            if (isActive && durationMs > 0) {
                Slider(
                    value = dragPositionMs ?: positionMs.toFloat(),
                    onValueChange = { dragPositionMs = it },
                    onValueChangeFinished = {
                        dragPositionMs?.let { onSeek(it.toInt()) }
                        dragPositionMs = null
                    },
                    valueRange = 0f..durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
}

private fun extractDuration(path: String): String {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        retriever.release()
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        "%d:%02d".format(minutes, seconds)
    } catch (e: Exception) {
        "--:--"
    }
}
