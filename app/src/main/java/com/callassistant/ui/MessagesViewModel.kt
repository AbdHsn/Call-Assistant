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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeleteProgress(val current: Int, val total: Int)

data class MessagesUiState(
    val smsMessages: List<SmsMessage> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val deleteProgress: DeleteProgress? = null
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _deleteProgress = MutableStateFlow<DeleteProgress?>(null)

    val uiState: StateFlow<MessagesUiState> = combine(
        smsRepository.observeSmsMessages(),
        contactRepository.observeContacts(),
        _deleteProgress
    ) { smsMessages, contacts, deleteProgress ->
        MessagesUiState(
            smsMessages = smsMessages,
            contacts = contacts,
            deleteProgress = deleteProgress
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MessagesUiState())

    fun syncSms() {
        viewModelScope.launch(Dispatchers.IO) {
            smsRepository.syncSms()
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
