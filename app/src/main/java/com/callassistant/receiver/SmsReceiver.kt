package com.callassistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.callassistant.CallAssistantApplication
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        val app = context.applicationContext as CallAssistantApplication
        val number = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody }

        val matchedRule = runBlocking(Dispatchers.IO) {
            app.spamRuleRepository.matchesAny(body)
        }

        if (matchedRule != null) {
            // Best-effort: aborting SMS_RECEIVED only works when app is default SMS handler.
            abortBroadcast()
        }

        CoroutineScope(Dispatchers.IO).launch {
            app.database.smsDao().insert(
                SmsMessage(
                    number = number,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    direction = SmsDirection.IN
                )
            )
        }
    }
}
