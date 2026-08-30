package com.callassistant.incall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callassistant.di.AppEntryPoint
import com.callassistant.util.CallNotificationManager
import dagger.hilt.android.EntryPointAccessors

/**
 * Receiver that handles Answer/Decline actions on the incoming call notification.
 *
 * These are normal broadcast PendingIntents because actions from a notification cannot
 * directly invoke methods on the active [InCallService]; this receiver bridges the gap.
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        CallNotificationManager.cancelIncomingCallNotification(context)

        val session = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppEntryPoint::class.java
        ).callSessionManager()
        when (intent.action) {
            ACTION_ANSWER_CALL -> session.answer()
            ACTION_DECLINE_CALL -> session.reject()
        }
    }

    companion object {
        const val ACTION_ANSWER_CALL = "com.callassistant.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.callassistant.ACTION_DECLINE_CALL"
    }
}
