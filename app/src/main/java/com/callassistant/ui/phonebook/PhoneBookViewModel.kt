package com.callassistant.ui.phonebook

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.Contact
import com.callassistant.data.repository.CallLogRepository
import com.callassistant.data.repository.ContactRepository
import com.callassistant.data.repository.SpamRuleRepository
import com.callassistant.util.MissedCallReadStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Shared state for contacts, call log, and dial pad tabs. */
data class PhoneBookUiState(
    val contacts: List<Contact> = emptyList(),
    val callLogs: List<CallLogEntry> = emptyList(),
    val contactsLoading: Boolean = true,
    val callLogsLoading: Boolean = true,
    val contactsSyncing: Boolean = false,
    val callLogsSyncing: Boolean = false,
    val unseenMissedCount: Int = 0,
    val missedCallsLastSeenAt: Long = 0L
) {
    val showContactsSkeleton: Boolean
        get() = contactsLoading || (contactsSyncing && contacts.isEmpty())

    val showCallLogsSkeleton: Boolean
        get() = callLogsLoading || (callLogsSyncing && callLogs.isEmpty())
}

@HiltViewModel
class PhoneBookViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository,
    private val spamRuleRepository: SpamRuleRepository
) : ViewModel() {

    private val contactsSyncing = MutableStateFlow(false)
    private val callLogsSyncing = MutableStateFlow(false)
    private val contactsReady = MutableStateFlow(false)
    private val callLogsReady = MutableStateFlow(false)
    private val missedReadRevision = MutableStateFlow(0)

    private var stickyContacts: List<Contact> = emptyList()
    private var stickyCallLogs: List<CallLogEntry> = emptyList()

    private var didAutoSyncContacts = false
    private var didAutoSyncCallLogs = false

    val uiState: StateFlow<PhoneBookUiState> = combine(
        combine(
            contactRepository.observeContacts().onEach { contactsReady.value = true },
            callLogRepository.observeCallLogs().onEach { callLogsReady.value = true },
            contactsSyncing,
            callLogsSyncing
        ) { contacts, callLogs, syncingContacts, syncingCallLogs ->
            DataBundle(contacts, callLogs, syncingContacts, syncingCallLogs)
        },
        contactsReady,
        callLogsReady,
        missedReadRevision
    ) { bundle, readyContacts, readyCallLogs, _ ->
        val (contacts, callLogs, syncingContacts, syncingCallLogs) = bundle
        if (contacts.isNotEmpty()) stickyContacts = contacts
        if (callLogs.isNotEmpty()) stickyCallLogs = callLogs

        val displayContacts = if (syncingContacts && contacts.isEmpty() && stickyContacts.isNotEmpty()) {
            stickyContacts
        } else {
            contacts
        }
        val displayCallLogs = if (syncingCallLogs && callLogs.isEmpty() && stickyCallLogs.isNotEmpty()) {
            stickyCallLogs
        } else {
            callLogs
        }

        val lastSeenAt = MissedCallReadStore.getLastSeenAt(context)
        val unseenMissedCount = MissedCallReadStore.getUnseenMissedCalls(context, displayCallLogs).size

        PhoneBookUiState(
            contacts = displayContacts,
            callLogs = displayCallLogs,
            contactsLoading = !readyContacts,
            callLogsLoading = !readyCallLogs,
            contactsSyncing = syncingContacts,
            callLogsSyncing = syncingCallLogs,
            unseenMissedCount = unseenMissedCount,
            missedCallsLastSeenAt = lastSeenAt
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PhoneBookUiState())

    fun ensureContactsSynced() {
        if (didAutoSyncContacts) return
        didAutoSyncContacts = true
        syncContacts()
    }

    fun ensureCallLogsSynced() {
        if (didAutoSyncCallLogs) return
        didAutoSyncCallLogs = true
        syncCallLogs()
    }

    fun syncContacts() {
        viewModelScope.launch {
            contactsSyncing.value = true
            try {
                withContext(Dispatchers.IO) { contactRepository.syncContacts() }
            } finally {
                contactsSyncing.value = false
            }
        }
    }

    fun syncCallLogs() {
        viewModelScope.launch {
            callLogsSyncing.value = true
            try {
                withContext(Dispatchers.IO) { callLogRepository.syncCallLogs() }
            } finally {
                callLogsSyncing.value = false
            }
        }
    }

    fun deleteContacts(contacts: List<Contact>) {
        viewModelScope.launch(Dispatchers.IO) { contactRepository.deleteContacts(contacts) }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) { contactRepository.deleteContacts(listOf(contact)) }
    }

    fun deleteCallLogs(entries: List<CallLogEntry>) {
        viewModelScope.launch(Dispatchers.IO) { callLogRepository.deleteCallLogs(entries) }
    }

    fun saveContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) { contactRepository.saveContact(contact) }
    }

    fun importContacts(contacts: List<Contact>) {
        viewModelScope.launch(Dispatchers.IO) { contactRepository.importContacts(contacts) }
    }

    fun blockNumber(number: String, reason: String = "Manually blocked", name: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            spamRuleRepository.blockNumber(number, reason, name)
        }
    }

    fun markAllMissedCallsSeen() {
        viewModelScope.launch(Dispatchers.IO) {
            MissedCallReadStore.markAllSeen(context, uiState.value.callLogs)
            missedReadRevision.value++
        }
    }
}

private data class DataBundle(
    val contacts: List<Contact>,
    val callLogs: List<CallLogEntry>,
    val contactsSyncing: Boolean,
    val callLogsSyncing: Boolean
)
