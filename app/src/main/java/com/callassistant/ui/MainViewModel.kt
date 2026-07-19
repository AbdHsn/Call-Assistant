package com.callassistant.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.CallAssistantApplication
import com.callassistant.data.entity.RuleType
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import com.callassistant.data.entity.SpamRule
import com.callassistant.data.sync.CallLogSyncer
import com.callassistant.data.sync.ContactSyncer
import com.callassistant.data.sync.SmsSyncer
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

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _selectedRoute = MutableStateFlow("contacts")
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
                db.spamRuleDao().insert(
                    SpamRule(
                        pattern = "^\\+?880?1[3-9]\\d{8}$",
                        type = RuleType.REGEX,
                        label = "Unverified BD mobile pattern"
                    )
                )
                db.spamRuleDao().insert(
                    SpamRule(
                        pattern = "+1900",
                        type = RuleType.PREFIX,
                        label = "Premium/scam prefix"
                    )
                )
                db.spamRuleDao().insert(
                    SpamRule(
                        pattern = "000000000",
                        type = RuleType.EXACT,
                        label = "Known spam"
                    )
                )
            }
        }
    }

    fun syncContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            val contacts = contactSyncer.sync()
            db.contactDao().deleteAll()
            db.contactDao().insertAll(contacts)
            _syncState.value = SyncState.Idle
        }
    }

    fun syncCallLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            val logs = callLogSyncer.sync()
            db.callLogDao().deleteAll()
            db.callLogDao().insertAll(logs)
            _syncState.value = SyncState.Idle
        }
    }

    fun syncSms() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            val messages = smsSyncer.sync()
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

    fun hasPermission(permission: String): Boolean {
        return getApplication<Application>().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
    }
}
