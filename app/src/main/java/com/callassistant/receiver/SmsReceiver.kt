package com.callassistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.callassistant.CallAssistantApplication
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import com.callassistant.util.CallNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION
        ) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        val app = context.applicationContext as CallAssistantApplication
        val number = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody }

        val isSenderBlocked = runBlocking(Dispatchers.IO) {
            app.spamRuleRepository.isNumberBlocked(number)
        }
        val matchedRule = if (isSenderBlocked) null else runBlocking(Dispatchers.IO) {
            app.spamRuleRepository.matchesAny(body)
        }

        if (isSenderBlocked || matchedRule != null) {
            // Best-effort: aborting SMS_RECEIVED only works when app is default SMS handler.
            abortBroadcast()
        }

        if (isSenderBlocked) return

        CoroutineScope(Dispatchers.IO).launch {
            app.database.smsDao().insert(
                SmsMessage(
                    number = number,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    direction = SmsDirection.IN
                )
            )
            CallNotificationManager.showSmsNotification(context, number, null, body)
        }
    }
}
