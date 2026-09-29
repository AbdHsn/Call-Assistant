package com.callassistant.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object CallRecordingPolicy {

    private const val PREFS_NAME = "recorder_settings"

    fun hasRecordAudioPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun shouldAutoRecord(
        context: Context,
        number: String,
        isIncoming: Boolean
    ): Boolean {
        if (!hasRecordAudioPermission(context)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("auto_record_enabled", false)) return false

        val hasNumber = number.isNotBlank()
        val recordIncoming = prefs.getBoolean("record_incoming", false)
        val recordOutgoing = prefs.getBoolean("record_outgoing", false)
        val recordUnknown = prefs.getBoolean("record_unknown", false)

        return when {
            !hasNumber -> recordOutgoing || recordUnknown
            isIncoming -> recordIncoming || recordUnknown
            else -> recordOutgoing || recordUnknown
        }
    }
}
