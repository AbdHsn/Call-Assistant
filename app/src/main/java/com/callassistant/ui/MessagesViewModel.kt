package com.callassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import com.callassistant.data.repository.ContactRepository
import com.callassistant.data.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeleteProgress(val current: Int, val total: Int)

data class MessagesUiState(
    val smsMessages: List<SmsMessage> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val deleteProgress: DeleteProgress? = null,
    val messagesLoading: Boolean = true,
    val messagesSyncing: Boolean = false
) {
    val showMessagesSkeleton: Boolean
        get() = messagesLoading || (messagesSyncing && smsMessages.isEmpty())
}

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _deleteProgress = MutableStateFlow<DeleteProgress?>(null)
    private val messagesSyncing = MutableStateFlow(false)
    private val messagesReady = MutableStateFlow(false)
    private val _pendingThreadNumber = MutableStateFlow<String?>(null)
    private var stickyMessages: List<SmsMessage> = emptyList()
    private var didAutoSyncSms = false

    val pendingThreadNumber: StateFlow<String?> = _pendingThreadNumber.asStateFlow()

    fun openThread(number: String) {
        _pendingThreadNumber.value = number
    }

    fun clearPendingThread() {
        _pendingThreadNumber.value = null
    }

    val uiState: StateFlow<MessagesUiState> = combine(
        smsRepository.observeSmsMessages().onEach { messagesReady.value = true },
        contactRepository.observeContacts(),
        _deleteProgress,
        messagesSyncing,
        messagesReady
    ) { smsMessages, contacts, deleteProgress, syncing, ready ->
        if (smsMessages.isNotEmpty()) stickyMessages = smsMessages

        val displayMessages = if (syncing && smsMessages.isEmpty() && stickyMessages.isNotEmpty()) {
            stickyMessages
        } else {
            smsMessages
        }

        MessagesUiState(
            smsMessages = displayMessages,
            contacts = contacts,
            deleteProgress = deleteProgress,
            messagesLoading = !ready,
            messagesSyncing = syncing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessagesUiState())

    fun ensureSmsSynced() {
        if (didAutoSyncSms) return
        didAutoSyncSms = true
        syncSms()
    }

    fun syncSms() {
        viewModelScope.launch {
            messagesSyncing.value = true
            try {
                withContext(Dispatchers.IO) { smsRepository.syncSms() }
            } finally {
                messagesSyncing.value = false
            }
        }
    }

    fun saveMessage(number: String, body: String, direction: SmsDirection) {
        viewModelScope.launch(Dispatchers.IO) {
            smsRepository.saveMessage(number, body, direction)
        }
    }

    fun deleteMessages(messages: List<SmsMessage>) {
        viewModelScope.launch(Dispatchers.IO) {
            smsRepository.deleteMessages(messages) { deleted, total ->
                _deleteProgress.value = DeleteProgress(deleted, total)
            }
            _deleteProgress.value = null
        }
    }
}
