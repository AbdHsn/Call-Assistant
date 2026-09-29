package com.callassistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.callassistant.service.CallRecordingService
import com.callassistant.util.CallRecordingPolicy
import com.callassistant.util.DefaultDialerUtils

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        // InCallService already owns recording when we are the default dialer.
        if (DefaultDialerUtils.isDefaultDialer(context)) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                CallRecordingService.setNumber(context, number)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val isIncoming = number.isNotBlank() && number != "null"
                if (CallRecordingPolicy.shouldAutoRecord(context, number, isIncoming)) {
                    CallRecordingService.startFromBackground(context, number, isIncoming)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                CallRecordingService.stop(context)
            }
        }
    }
}
