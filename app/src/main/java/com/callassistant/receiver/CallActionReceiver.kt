package com.callassistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callassistant.service.CallAssistantInCallService
import com.callassistant.util.CallNotificationManager

/**
 * Receiver that handles Answer/Decline actions on the incoming call notification.
 *
 * These are normal broadcast PendingIntents because actions from a notification cannot
 * directly invoke methods on the active [InCallService]; this receiver bridges the gap.
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        CallNotificationManager.cancelIncomingCallNotification(context)

        when (intent.action) {
            ACTION_ANSWER_CALL -> CallAssistantInCallService.activeCall?.answer(0)
            ACTION_DECLINE_CALL -> CallAssistantInCallService.activeCall?.reject(false, null)
        }
    }

    companion object {
        const val ACTION_ANSWER_CALL = "com.callassistant.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.callassistant.ACTION_DECLINE_CALL"
    }
}
