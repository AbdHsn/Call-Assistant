package com.callassistant.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal local persistence for quick notes taken during a call, keyed by phone number.
 */
object CallNotesStore {

    private const val PREFS_NAME = "call_notes_store"
    private const val KEY_NOTES = "notes_by_number"

    fun addNote(context: Context, number: String, note: String) {
        if (note.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val root = JSONObject(prefs.getString(KEY_NOTES, null) ?: "{}")
        val existing = root.optJSONArray(number) ?: JSONArray()
        existing.put(
            JSONObject().apply {
                put("text", note)
                put("timestamp", System.currentTimeMillis())
            }
        )
        root.put(number, existing)
        prefs.edit().putString(KEY_NOTES, root.toString()).apply()
    }

    fun getNotes(context: Context, number: String): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val root = JSONObject(prefs.getString(KEY_NOTES, null) ?: "{}")
        val array = root.optJSONArray(number) ?: return emptyList()
        return (0 until array.length()).map { array.getJSONObject(it).getString("text") }
    }

    fun getAllNotes(context: Context): List<Pair<String, List<CallNote>>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val root = JSONObject(prefs.getString(KEY_NOTES, null) ?: "{}")
        val sorted = sortedMapOf<String, List<CallNote>>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val number = keys.next()
            val array = root.optJSONArray(number) ?: continue
            val list = (0 until array.length()).map {
                val obj = array.getJSONObject(it)
                CallNote(
                    text = obj.getString("text"),
                    timestamp = obj.optLong("timestamp", 0L)
                )
            }
            if (list.isNotEmpty()) sorted[number] = list
        }
        return sorted.toList()
    }
}

data class CallNote(
    val text: String,
    val timestamp: Long
)
