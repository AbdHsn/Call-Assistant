package com.callassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.Contact
import com.callassistant.data.repository.CallLogRepository
import com.callassistant.data.repository.ContactRepository
import com.callassistant.data.repository.SpamRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CallLogUiState(
    val callLogs: List<CallLogEntry> = emptyList(),
    val contacts: List<Contact> = emptyList()
)

@HiltViewModel
class CallLogViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val contactRepository: ContactRepository,
    private val spamRuleRepository: SpamRuleRepository
) : ViewModel() {

    val uiState: StateFlow<CallLogUiState> = combine(
        callLogRepository.observeCallLogs(),
        contactRepository.observeContacts()
    ) { callLogs, contacts ->
        CallLogUiState(callLogs = callLogs, contacts = contacts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallLogUiState())

    fun syncCallLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            callLogRepository.syncCallLogs()
        }
    }

    fun deleteCallLogs(entries: List<CallLogEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            callLogRepository.deleteCallLogs(entries)
        }
    }

    fun saveContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.saveContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.deleteContacts(listOf(contact))
        }
    }

    fun blockNumber(number: String, reason: String = "Manually blocked", name: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            spamRuleRepository.blockNumber(number, reason, name)
        }
    }
}
