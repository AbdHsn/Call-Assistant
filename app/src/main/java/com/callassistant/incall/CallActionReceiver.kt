package com.callassistant.incall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callassistant.di.AppEntryPoint
import com.callassistant.util.CallNotificationManager
import com.callassistant.util.CallPlacer
import dagger.hilt.android.EntryPointAccessors

/**
 * Receiver that handles Answer/Decline actions on the incoming call notification.
 *
 * These are normal broadcast PendingIntents because actions from a notification cannot
 * directly invoke methods on the active [InCallService]; this receiver bridges the gap.
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER_CALL, ACTION_DECLINE_CALL -> {
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
            ACTION_CALL_BACK -> {
                val number = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
                CallPlacer.placeCallWithFeedback(context, number)
            }
        }
    }

    companion object {
        const val ACTION_ANSWER_CALL = "com.callassistant.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.callassistant.ACTION_DECLINE_CALL"
        const val ACTION_CALL_BACK = "com.callassistant.ACTION_CALL_BACK"
        const val EXTRA_NUMBER = "number"
    }
}
