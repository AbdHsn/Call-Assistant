package com.callassistant.util

import android.content.Context

/**
 * Persists keys of call-log/SMS entries the user deleted in-app, so that resyncing from the
 * system provider (which may silently fail to actually delete the underlying row, e.g. when
 * this app isn't the default SMS/dialer app) never brings them back into the local database.
 */
object DeletedEntriesStore {

    private const val PREFS_NAME = "deleted_entries_store"
    private const val KEY_CALL_LOGS = "deleted_call_log_keys"
    private const val KEY_SMS = "deleted_sms_keys"
    private const val KEY_CONTACTS = "deleted_contact_keys"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeletedCallLogKeys(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_CALL_LOGS, emptySet()) ?: emptySet()

    fun addDeletedCallLogKeys(context: Context, keys: Collection<String>) {
        val p = prefs(context)
        val updated = (p.getStringSet(KEY_CALL_LOGS, emptySet()) ?: emptySet()).toMutableSet()
        updated.addAll(keys)
        p.edit().putStringSet(KEY_CALL_LOGS, updated).apply()
    }

    fun getDeletedSmsKeys(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SMS, emptySet()) ?: emptySet()

    fun addDeletedSmsKeys(context: Context, keys: Collection<String>) {
        val p = prefs(context)
        val updated = (p.getStringSet(KEY_SMS, emptySet()) ?: emptySet()).toMutableSet()
        updated.addAll(keys)
        p.edit().putStringSet(KEY_SMS, updated).apply()
    }

    fun getDeletedContactKeys(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_CONTACTS, emptySet()) ?: emptySet()

    fun addDeletedContactKeys(context: Context, keys: Collection<String>) {
        val p = prefs(context)
        val updated = (p.getStringSet(KEY_CONTACTS, emptySet()) ?: emptySet()).toMutableSet()
        updated.addAll(keys)
        p.edit().putStringSet(KEY_CONTACTS, updated).apply()
    }
}
