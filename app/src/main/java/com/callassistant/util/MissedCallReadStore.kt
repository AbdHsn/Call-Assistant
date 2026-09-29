package com.callassistant.util

import android.content.Context
import com.callassistant.data.db.AppDatabase
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.CallType
import org.json.JSONArray
import org.json.JSONObject

data class UnseenMissedCall(
    val number: String,
    val displayName: String?,
    val timestamp: Long
)

/**
 * Tracks which missed calls the user has acknowledged.
 *
 * Uses a [lastSeenAt] watermark instead of per-row flags so call-log resync
 * (delete-all + reinsert) does not reset read state.
 */
object MissedCallReadStore {

    private const val PREFS_NAME = "missed_call_read_store"
    private const val KEY_LAST_SEEN_AT = "last_seen_at"
    private const val KEY_INITIALIZED = "initialized"
    private const val KEY_PENDING = "pending_missed_calls"

    fun getLastSeenAt(context: Context): Long =
        prefs(context).getLong(KEY_LAST_SEEN_AT, 0L)

    fun initializeIfNeeded(context: Context, callLogs: List<CallLogEntry>) {
        val p = prefs(context)
        if (p.getBoolean(KEY_INITIALIZED, false)) return
        markAllSeen(context, callLogs)
        p.edit().putBoolean(KEY_INITIALIZED, true).apply()
    }

    fun recordMissedCall(
        context: Context,
        number: String,
        displayName: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (number.isBlank()) return
        val pending = loadPending(context).toMutableList()
        val key = missedKey(number, timestamp)
        if (pending.none { missedKey(it.number, it.timestamp) == key }) {
            pending.add(UnseenMissedCall(number, displayName, timestamp))
            savePending(context, pending)
        }
    }

    fun getUnseenMissedCalls(
        context: Context,
        callLogs: List<CallLogEntry> = emptyList()
    ): List<UnseenMissedCall> {
        val lastSeenAt = getLastSeenAt(context)
        val fromLogs = callLogs
            .asSequence()
            .filter { it.type == CallType.MISSED && it.timestamp > lastSeenAt }
            .map { UnseenMissedCall(it.number, it.name, it.timestamp) }
        val fromPending = loadPending(context).asSequence().filter { it.timestamp > lastSeenAt }
        return (fromLogs + fromPending)
            .distinctBy { missedKey(it.number, it.timestamp) }
            .sortedByDescending { it.timestamp }
            .toList()
    }

    fun markAllSeen(context: Context, callLogs: List<CallLogEntry> = emptyList()) {
        val pending = loadPending(context)
        val markUpTo = maxOf(
            System.currentTimeMillis(),
            callLogs.filter { it.type == CallType.MISSED }.maxOfOrNull { it.timestamp } ?: 0L,
            pending.maxOfOrNull { it.timestamp } ?: 0L
        )
        prefs(context).edit().putLong(KEY_LAST_SEEN_AT, markUpTo).apply()
        savePending(context, emptyList())
        CallNotificationManager.cancelMissedCallNotification(context)
    }

    suspend fun refreshNotification(context: Context, callLogs: List<CallLogEntry>? = null) {
        val logs = callLogs ?: AppDatabase.getDatabase(context).callLogDao().getMissedEntries()
        val unseen = getUnseenMissedCalls(context, logs)
        if (unseen.isEmpty()) {
            CallNotificationManager.cancelMissedCallNotification(context)
        } else {
            CallNotificationManager.refreshMissedCallNotification(context, unseen)
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun missedKey(number: String, timestamp: Long) = "$number|$timestamp"

    private fun loadPending(context: Context): List<UnseenMissedCall> {
        val raw = prefs(context).getString(KEY_PENDING, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        UnseenMissedCall(
                            number = item.getString("number"),
                            displayName = item.optString("displayName").takeIf { it.isNotBlank() },
                            timestamp = item.getLong("timestamp")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun savePending(context: Context, pending: List<UnseenMissedCall>) {
        val array = JSONArray()
        pending.forEach { call ->
            array.put(
                JSONObject()
                    .put("number", call.number)
                    .put("displayName", call.displayName ?: "")
                    .put("timestamp", call.timestamp)
            )
        }
        prefs(context).edit().putString(KEY_PENDING, array.toString()).apply()
    }
}
