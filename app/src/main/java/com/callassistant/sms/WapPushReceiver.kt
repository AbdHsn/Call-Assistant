package com.callassistant.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Required for this app to be eligible for the default SMS app role. This app does not
 * currently process MMS/WAP push content, so this is a minimal no-op receiver.
 */
class WapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: MMS/WAP push handling is not implemented.
    }
}
