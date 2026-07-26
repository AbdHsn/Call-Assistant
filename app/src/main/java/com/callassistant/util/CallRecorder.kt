package com.callassistant.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight wrapper around [MediaRecorder] used to capture microphone audio during a call.
 *
 * Note: standard Android APIs restrict capturing the remote party's voice directly
 * (AudioSource.VOICE_CALL is not available to third-party apps on modern Android).
 * This records from the device microphone, which works best with the speaker enabled.
 */
class CallRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var currentFilePath: String? = null
        private set

    val isRecording: Boolean
        get() = recorder != null

    fun start(number: String): Boolean {
        if (isRecording) return false
        val dir = File(context.getExternalFilesDir(null), "CallRecordings").apply { mkdirs() }
        val safeNumber = number.ifBlank { "unknown" }.replace(Regex("[^0-9+]"), "")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "call_${safeNumber}_$timestamp.m4a")

        return try {
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            currentFilePath = file.absolutePath
            true
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
        private val FILENAME_PATTERN = Regex("call_(.*)_(\\d{8}_\\d{6})\\.m4a")

        fun recordingsDir(context: Context): File =
            File(context.getExternalFilesDir(null), "CallRecordings")

        fun listRecordings(context: Context): List<Recording> {
            val dir = recordingsDir(context)
            val files = dir.listFiles { file -> file.extension == "m4a" } ?: emptyArray()
            return files.mapNotNull { file ->
                val match = FILENAME_PATTERN.find(file.name)
                val number = match?.groupValues?.get(1)?.ifBlank { "Unknown" } ?: "Unknown"
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
