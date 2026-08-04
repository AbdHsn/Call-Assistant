package com.callassistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.callassistant.service.CallRecordingService

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                CallRecordingService.setNumber(context, number)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (shouldStart(context, number)) {
                    CallRecordingService.start(context, number)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                CallRecordingService.stop(context)
            }
        }
    }

    private fun shouldStart(context: Context, number: String): Boolean {
        val prefs = context.getSharedPreferences("recorder_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("auto_record_enabled", true)) return false

        val hasNumber = number.isNotBlank() && number != "null"
        val recordIncoming = prefs.getBoolean("record_incoming", true)
        val recordOutgoing = prefs.getBoolean("record_outgoing", true)
        val recordUnknown = prefs.getBoolean("record_unknown", true)

        return when {
            !hasNumber -> recordOutgoing || recordUnknown
            else -> recordIncoming || recordUnknown
        }
    }
}
