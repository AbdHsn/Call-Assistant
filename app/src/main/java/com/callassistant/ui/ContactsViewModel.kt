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

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val callLogs: List<CallLogEntry> = emptyList()
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository,
    private val spamRuleRepository: SpamRuleRepository
) : ViewModel() {

    val uiState: StateFlow<ContactsUiState> = combine(
        contactRepository.observeContacts(),
        callLogRepository.observeCallLogs()
    ) { contacts, callLogs ->
        ContactsUiState(contacts = contacts, callLogs = callLogs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactsUiState())

    fun syncContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.syncContacts()
        }
    }

    fun deleteContacts(contacts: List<Contact>) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.deleteContacts(contacts)
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

    fun importContacts(contacts: List<Contact>) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.importContacts(contacts)
        }
    }

    fun blockNumber(number: String, reason: String = "Manually blocked", name: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            spamRuleRepository.blockNumber(number, reason, name)
        }
    }
}
