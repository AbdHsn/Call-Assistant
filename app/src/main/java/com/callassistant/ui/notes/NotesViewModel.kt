package com.callassistant.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.data.entity.Contact
import com.callassistant.data.repository.ContactRepository
import com.callassistant.data.repository.NotesRepository
import com.callassistant.util.CallNote
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val contacts: List<Contact> = emptyList(),
    val notesByNumber: List<Pair<String, List<CallNote>>> = emptyList()
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    contactRepository: ContactRepository,
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val notesFlow = kotlinx.coroutines.flow.MutableStateFlow(notesRepository.getAllNotes())

    val uiState: StateFlow<NotesUiState> = combine(
        contactRepository.observeContacts(),
        notesFlow
    ) { contacts, notes ->
        NotesUiState(contacts = contacts, notesByNumber = notes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesUiState())

    fun refreshNotes() {
        notesFlow.value = notesRepository.getAllNotes()
    }

    fun addNote(number: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            notesRepository.addNote(number, note)
            refreshNotes()
        }
    }
}
