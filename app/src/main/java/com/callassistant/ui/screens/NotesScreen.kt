package com.callassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.data.entity.Contact
import com.callassistant.ui.notes.NotesViewModel
import com.callassistant.util.CallNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class NotesSortMode(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first")
}

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contacts = uiState.contacts
    val allNotes = uiState.notesByNumber
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(NotesSortMode.NEWEST) }

    val trimmedQuery = query.trim().lowercase()
    val displayNotes = remember(allNotes, contacts, trimmedQuery, sortMode) {
        val list = allNotes.mapNotNull { (number, notes) ->
            val contact = contacts.find { it.phoneNumber == number }
            val contactName = contact?.name?.lowercase() ?: ""
            val filteredNotes = if (
                trimmedQuery.isEmpty() ||
                number.lowercase().contains(trimmedQuery) ||
                contactName.contains(trimmedQuery)
            ) {
                notes
            } else {
                notes.filter { it.text.lowercase().contains(trimmedQuery) }
            }
            if (filteredNotes.isEmpty()) return@mapNotNull null

            val sortedNotes = when (sortMode) {
                NotesSortMode.NEWEST -> filteredNotes.sortedByDescending { it.timestamp }
                NotesSortMode.OLDEST -> filteredNotes.sortedBy { it.timestamp }
            }
            Triple(number, sortedNotes, sortedNotes.maxOfOrNull { it.timestamp } ?: 0L)
        }
        val sorted = when (sortMode) {
            NotesSortMode.NEWEST -> list.sortedByDescending { it.third }
            NotesSortMode.OLDEST -> list.sortedBy { it.third }
        }
        sorted.map { it.first to it.second }
    }
    val totalNotes = remember(displayNotes) { displayNotes.sumOf { it.second.size } }

    if (allNotes.isEmpty()) {
        EmptyNotesState(modifier = modifier.fillMaxSize())
    } else if (displayNotes.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No matching notes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "Call Notes",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$totalNotes saved note${if (totalNotes == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search notes") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort"
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            NotesSortMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = {
                                        sortMode = mode
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            items(displayNotes, key = { it.first }) { (number, notes) ->
                val contact = remember(contacts, number) {
                    contacts.find { it.phoneNumber == number }
                }
                NotesContactCard(
                    number = number,
                    notes = notes,
                    contact = contact
                )
            }
        }
    }
}

@Composable
private fun NotesContactCard(
    number: String,
    notes: List<CallNote>,
    contact: Contact?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContactAvatar(
                    name = contact?.name ?: number,
                    photoUri = contact?.photoUri
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact?.name?.takeIf { it.isNotBlank() } ?: number,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (contact?.name?.isNotBlank() == true) {
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                NoteCountBadge(count = notes.size)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            notes.forEachIndexed { index, note ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = note.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = formatNoteTimestamp(note.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (index < notes.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCountBadge(count: Int) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun EmptyNotesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.EditNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = "No call notes yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Notes you save during a call will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp)
        )
    }
}

private fun formatNoteTimestamp(timestamp: Long): String {
    return if (timestamp > 0L) {
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            .format(Date(timestamp))
    } else {
        ""
    }
}
