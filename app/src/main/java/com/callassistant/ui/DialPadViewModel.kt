package com.callassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.Contact
import com.callassistant.data.repository.CallLogRepository
import com.callassistant.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DialPadUiState(
    val contacts: List<Contact> = emptyList(),
    val callLogs: List<CallLogEntry> = emptyList()
)

@HiltViewModel
class DialPadViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository
) : ViewModel() {

    val uiState: StateFlow<DialPadUiState> = combine(
        contactRepository.observeContacts(),
        callLogRepository.observeCallLogs()
    ) { contacts, callLogs ->
        DialPadUiState(contacts = contacts, callLogs = callLogs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DialPadUiState())

    fun saveContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.saveContact(contact)
        }
    }
}
