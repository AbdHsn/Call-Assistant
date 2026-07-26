package com.callassistant.ui

import android.app.Application
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.CallAssistantApplication
import com.callassistant.data.entity.BlockedNumber
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.RuleType
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import com.callassistant.data.entity.SpamRule
import com.callassistant.data.sync.CallLogSyncer
import com.callassistant.data.sync.ContactSyncer
import com.callassistant.data.sync.SmsSyncer
import com.callassistant.util.DeletedEntriesStore
import com.callassistant.util.PhoneNumberNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CallAssistantApplication
    private val db = app.database
    private val spamRepo = app.spamRuleRepository

    private val contactSyncer = ContactSyncer(application)
    private val callLogSyncer = CallLogSyncer(application)
    private val smsSyncer = SmsSyncer(application)

    val contacts = db.contactDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val callLogs = db.callLogDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val spamRules = db.spamRuleDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val smsMessages = db.smsDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val blockedNumbers = db.blockedNumberDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _deleteProgress = MutableStateFlow<DeleteProgress?>(null)
    val deleteProgress: StateFlow<DeleteProgress?> = _deleteProgress

    private val _selectedRoute = MutableStateFlow("call_log")
    val selectedRoute: StateFlow<String> = _selectedRoute

    init {
        seedSpamRules()
    }

    fun selectRoute(route: String) {
        _selectedRoute.value = route
    }

    private fun seedSpamRules() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.spamRuleDao().getAll().first()
            if (existing.isEmpty()) {
                // Blocking rules: numbers/messages matching these are auto-rejected.
                db.spamRuleDao().insert(
                    SpamRule(
                        pattern = "+1900",
                        type = RuleType.PREFIX,
                        label = "Premium/scam prefix",
                        isBlocking = true
                    )
                )
                db.spamRuleDao().insert(
                    SpamRule(
                        pattern = "^(\\d)\\1{6,}$",
                        type = RuleType.REGEX,
                        label = "Repeated-digit spoofed number",
                        isBlocking = true
                    )
                )
                db.spamRuleDao().insert(
                    SpamRule(
                        pattern = "^0*123456789\$|^0*987654321\$",
                        type = RuleType.REGEX,
                        label = "Sequential-digit spoofed number",
                        isBlocking = true
                    )
                )
                db.spamRuleDao().insert(
                    SpamRule(
                        pattern = "000000000",
                        type = RuleType.EXACT,
                        label = "Known spam number",
                        isBlocking = true
                    )
                )
                // Flag-only rules: surfaced to the user but calls/messages are still allowed through.
                db.spamRuleDao().insert(
                    SpamRule(
                        pattern = "^\\+?880?1[3-9]\\d{8}$",
                        type = RuleType.REGEX,
                        label = "Unverified BD mobile pattern",
                        isBlocking = false
                    )
                )
                db.spamRuleDao().insert(
                    SpamRule(
                        pattern = "you have won",
                        type = RuleType.PREFIX,
                        label = "Prize/lottery scam wording",
                        isBlocking = false
                    )
                )
            }
        }
    }

    fun blockNumber(number: String, reason: String = "Manually blocked", name: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalized = PhoneNumberNormalizer.normalize(number)
            db.blockedNumberDao().insert(BlockedNumber(number = normalized, name = name, reason = reason))
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.blockedNumberDao().delete(PhoneNumberNormalizer.normalize(number))
        }
    }

    private fun contactKey(phoneNumber: String) = phoneNumber

    fun syncContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            val deletedKeys = DeletedEntriesStore.getDeletedContactKeys(getApplication())
            val contacts = contactSyncer.sync()
                .filterNot { contactKey(it.phoneNumber) in deletedKeys }
            db.contactDao().deleteAll()
            db.contactDao().insertAll(contacts)
            _syncState.value = SyncState.Idle
        }
    }

    fun deleteContacts(contacts: List<com.callassistant.data.entity.Contact>) {
        viewModelScope.launch(Dispatchers.IO) {
            DeletedEntriesStore.addDeletedContactKeys(
                getApplication(),
                contacts.map { contactKey(it.phoneNumber) }
            )
            val resolver = getApplication<Application>().contentResolver
            contacts.forEach { contact ->
                try {
                    val encodedPhone = Uri.encode(contact.phoneNumber)
                    val lookupUri = Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI, encodedPhone
                    )
                    val seen = mutableSetOf<Long>()
                    resolver.query(
                        lookupUri,
                        arrayOf(ContactsContract.PhoneLookup.CONTACT_ID),
                        null, null, null
                    )?.use { cursor ->
                        val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID)
                        while (cursor.moveToNext()) {
                            val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                            if (seen.add(id)) {
                                resolver.delete(
                                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id),
                                    null, null
                                )
                            }
                        }
                    }
                } catch (_: SecurityException) {}
            }
            val updated = contactSyncer.sync()
            db.contactDao().deleteAll()
            db.contactDao().insertAll(updated)
        }
    }

    fun saveContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            db.contactDao().insertAll(listOf(contact))
        }
    }

    private fun callLogKey(number: String, timestamp: Long) = "$number|$timestamp"

    fun syncCallLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            val deletedKeys = DeletedEntriesStore.getDeletedCallLogKeys(getApplication())
            val logs = callLogSyncer.sync()
                .filterNot { callLogKey(it.number, it.timestamp) in deletedKeys }
            db.callLogDao().deleteAll()
            db.callLogDao().insertAll(logs)
            _syncState.value = SyncState.Idle
        }
    }

    fun deleteCallLogs(entries: List<CallLogEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            DeletedEntriesStore.addDeletedCallLogKeys(
                getApplication(),
                entries.map { callLogKey(it.number, it.timestamp) }
            )
            val resolver = getApplication<Application>().contentResolver
            entries.forEach { entry ->
                try {
                    resolver.delete(
                        CallLog.Calls.CONTENT_URI,
                        "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ?",
                        arrayOf(entry.number, entry.timestamp.toString())
                    )
                } catch (_: SecurityException) {}
            }
            db.callLogDao().deleteByIds(entries.map { it.id })
        }
    }

    private fun smsKey(number: String, timestamp: Long, body: String) = "$number|$timestamp|$body"

    fun syncSms() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            val deletedKeys = DeletedEntriesStore.getDeletedSmsKeys(getApplication())
            val messages = smsSyncer.sync()
                .filterNot { smsKey(it.number, it.timestamp, it.body) in deletedKeys }
            db.smsDao().deleteAll()
            db.smsDao().insertAll(messages)
            _syncState.value = SyncState.Idle
        }
    }

    fun addSpamRule(pattern: String, type: RuleType, label: String, isBlocking: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            db.spamRuleDao().insert(
                SpamRule(pattern = pattern, type = type, label = label, isBlocking = isBlocking)
            )
        }
    }

    fun deleteSpamRule(rule: SpamRule) {
        viewModelScope.launch(Dispatchers.IO) {
            db.spamRuleDao().delete(rule)
        }
    }

    fun saveMessage(number: String, body: String, direction: SmsDirection) {
        viewModelScope.launch(Dispatchers.IO) {
            db.smsDao().insert(
                SmsMessage(number = number, body = body, timestamp = System.currentTimeMillis(), direction = direction)
            )
        }
    }

    fun deleteMessages(messages: List<SmsMessage>) {
        viewModelScope.launch(Dispatchers.IO) {
            val total = messages.size
            _deleteProgress.value = DeleteProgress(0, total)
            val resolver = getApplication<Application>().contentResolver
            messages.forEachIndexed { index, message ->
                DeletedEntriesStore.addDeletedSmsKeys(
                    getApplication(),
                    listOf(smsKey(message.number, message.timestamp, message.body))
                )
                try {
                    resolver.delete(
                        android.provider.Telephony.Sms.CONTENT_URI,
                        "${android.provider.Telephony.Sms.ADDRESS} = ? AND ${android.provider.Telephony.Sms.DATE} = ?",
                        arrayOf(message.number, message.timestamp.toString())
                    )
                } catch (_: SecurityException) {
                }
                db.smsDao().deleteByIds(listOf(message.id))
                _deleteProgress.value = DeleteProgress(index + 1, total)
            }
            _deleteProgress.value = null
        }
    }

    fun hasPermission(permission: String): Boolean {
        return getApplication<Application>().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
    }

    data class DeleteProgress(val current: Int, val total: Int)
}
