package com.callassistant.data.sync

import android.content.ContentResolver
import android.content.Context
import android.provider.CallLog
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogSyncer(private val context: Context) {

    suspend fun sync(): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CallLogEntry>()
        val resolver: ContentResolver = context.contentResolver
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE
        )
        try {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
                while (cursor.moveToNext()) {
                    val rawNumber = if (numberIndex >= 0) cursor.getString(numberIndex) else null
                    val number = rawNumber?.replace(" ", "")?.replace("-", "")?.trim() ?: continue
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val type = if (typeIndex >= 0) {
                        when (cursor.getInt(typeIndex)) {
                            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                            CallLog.Calls.REJECTED_TYPE -> CallType.MISSED
                            else -> CallType.INCOMING
                        }
                    } else CallType.INCOMING
                    val date = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L
                    entries.add(CallLogEntry(number = number, name = name, type = type, timestamp = date))
                }
            }
        } catch (_: SecurityException) {
            // permission not granted
        }
        entries
    }
}
