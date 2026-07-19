package com.callassistant.data.sync

import android.content.ContentResolver
import android.content.Context
import android.provider.Telephony
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsSyncer(private val context: Context) {

    suspend fun sync(): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()
        val resolver: ContentResolver = context.contentResolver
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        resolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE)
            while (cursor.moveToNext()) {
                val number = cursor.getString(addressIndex)?.replace(" ", "") ?: continue
                val body = cursor.getString(bodyIndex) ?: ""
                val date = cursor.getLong(dateIndex)
                val direction = when (cursor.getInt(typeIndex)) {
                    Telephony.Sms.MESSAGE_TYPE_SENT -> SmsDirection.OUT
                    else -> SmsDirection.IN
                }
                messages.add(SmsMessage(number = number, body = body, timestamp = date, direction = direction))
            }
        }
        messages
    }
}
