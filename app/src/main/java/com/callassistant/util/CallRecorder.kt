package com.callassistant.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight wrapper around [MediaRecorder] used to capture call audio.
 *
 * It first attempts [MediaRecorder.AudioSource.VOICE_CALL], which captures both
 * uplink and downlink. This requires the system-granted [android.Manifest.permission.CAPTURE_AUDIO_OUTPUT]
 * permission and is normally unavailable to third-party apps; if it fails, it falls back
 * to microphone-based sources that record the remote party only when the speakerphone is on.
 */
class CallRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var currentFilePath: String? = null
        private set

    val isRecording: Boolean
        get() = recorder != null

    fun start(number: String): Boolean {
        if (isRecording) return false
        val dir = recordingsDir(context).apply { mkdirs() }
        val safeNumber = number.ifBlank { "unknown" }.replace(Regex("[^0-9+]"), "")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "${timestamp}_${safeNumber}.m4a")

        fun createRecorder(): MediaRecorder {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
        }

        fun trySource(source: Int): MediaRecorder? {
            val mediaRecorder = createRecorder()
            return try {
                mediaRecorder.setAudioSource(source)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder.setAudioSamplingRate(44100)
                mediaRecorder.setAudioEncodingBitRate(128000)
                mediaRecorder.setOutputFile(file.absolutePath)
                mediaRecorder.prepare()
                mediaRecorder.start()
                mediaRecorder
            } catch (e: Exception) {
                mediaRecorder.release()
                if (file.exists()) file.delete()
                null
            }
        }

        val prefs = context.getSharedPreferences("recorder_settings", Context.MODE_PRIVATE)
        val override = prefs.getString("audio_source_override", null)?.toIntOrNull()
        val orderedSources = if (override != null) {
            listOf(override)
        } else {
            val saved = prefs.getString("audio_source", null)?.toIntOrNull()
            if (saved != null) {
                listOf(saved) + listOf(
                    MediaRecorder.AudioSource.VOICE_CALL,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MediaRecorder.AudioSource.MIC
                ).filter { it != saved }
            } else {
                listOf(
                    MediaRecorder.AudioSource.VOICE_CALL,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MediaRecorder.AudioSource.MIC
                )
            }
        }

        return try {
            var success = false
            for (source in orderedSources) {
                val mediaRecorder = trySource(source)
                if (mediaRecorder != null) {
                    recorder = mediaRecorder
                    currentFilePath = file.absolutePath
                    prefs.edit().putString("audio_source", source.toString()).apply()
                    success = true
                    break
                }
            }
            success
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            currentFilePath = null
            false
        }
    }

    fun stop(): String? {
        val path = currentFilePath
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore: recorder may already be in an invalid state.
        }
        recorder = null
        currentFilePath = null
        return path
    }

    companion object {
        private val FILENAME_PATTERN = Regex("(\\d{8}_\\d{6})_(.*)\\.m4a")

        fun recordingsDir(context: Context): File =
            File(context.getExternalFilesDir(null), "CallRecordings")

        fun listRecordings(context: Context): List<Recording> {
            val dir = recordingsDir(context)
            val files = dir.listFiles { file -> file.extension == "m4a" } ?: emptyArray()
            return files.mapNotNull { file ->
                val match = FILENAME_PATTERN.find(file.name)
                val number = match?.groupValues?.get(2)?.ifBlank { "Unknown" } ?: "Unknown"
                Recording(file = file, number = number, timestamp = file.lastModified())
            }.sortedByDescending { it.timestamp }
        }
    }
}

data class Recording(
    val file: File,
    val number: String,
    val timestamp: Long
)
