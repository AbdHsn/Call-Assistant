package com.callassistant.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import com.callassistant.di.AppEntryPoint
import com.callassistant.util.CallNotificationManager
import dagger.hilt.android.EntryPointAccessors
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

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppEntryPoint::class.java
        )
        val repo = entryPoint.spamRuleRepository()
        val db = entryPoint.appDatabase()

        val number = messages.firstOrNull()?.originatingAddress?.replace(" ", "")?.trim() ?: return
        val body = messages.joinToString("") { it.messageBody }
        val timestamp = System.currentTimeMillis()
        val dateSent = messages.firstOrNull()?.timestampMillis ?: timestamp

        val isSenderBlocked = runBlocking(Dispatchers.IO) {
            repo.isNumberBlocked(number)
        }
        val matchedRule = if (isSenderBlocked) null else runBlocking(Dispatchers.IO) {
            repo.matchesAny(body)
        }

        if (isSenderBlocked || matchedRule != null) {
            // Best-effort: aborting SMS_RECEIVED only works when app is default SMS handler.
            abortBroadcast()
        }

        if (isSenderBlocked) return

        CoroutineScope(Dispatchers.IO).launch {
            db.smsDao().insert(
                SmsMessage(
                    number = number,
                    body = body,
                    timestamp = timestamp,
                    direction = SmsDirection.IN
                )
            )
            try {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, number)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, timestamp)
                    put(Telephony.Sms.DATE_SENT, dateSent)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                    put(Telephony.Sms.READ, 0)
                }
                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            } catch (_: SecurityException) {
                // Not the default SMS app; the default app will write to the provider.
            }
            CallNotificationManager.showSmsNotification(context, number, null, body)
        }
    }
}
