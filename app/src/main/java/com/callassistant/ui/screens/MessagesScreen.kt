package com.callassistant.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.permission.Permissions
import com.callassistant.ui.MainViewModel
import com.callassistant.ui.components.PermissionGuard
import com.callassistant.data.entity.SmsMessage
import java.util.Calendar

private enum class MessageSortMode(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A")
}

private enum class MessageDayCategory { TODAY, YESTERDAY, OLDER }

private data class ThreadSummary(
    val number: String,
    val name: String,
    val lastBody: String,
    val lastTimestamp: Long,
    val messages: List<SmsMessage> = emptyList()
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessagesScreen(
    viewModel: MainViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedThread by remember { mutableStateOf<String?>(null) }
    var showNewMessage by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        PermissionGuard(
            permission = Permissions.readSms,
            hasPermission = hasPermission,
            requestPermissions = requestPermissions,
            modifier = Modifier.fillMaxSize()
        ) {
            val messages by viewModel.smsMessages.collectAsStateWithLifecycle()
            val contacts by viewModel.contacts.collectAsStateWithLifecycle()
            var searchQuery by remember { mutableStateOf("") }
            var sortMode by remember { mutableStateOf(MessageSortMode.NEWEST) }
            var selectedNumbers by remember { mutableStateOf(setOf<String>()) }
            val inSelectionMode = selectedNumbers.isNotEmpty()
            var showDeleteDialog by remember { mutableStateOf(false) }
            val deleteProgress by viewModel.deleteProgress.collectAsStateWithLifecycle()
            val hasReadSms = hasPermission(Permissions.readSms.permission)

            val threads by remember(messages, contacts, searchQuery, sortMode) {
                derivedStateOf {
                    messages
                        .groupBy { it.number }
                        .map { (number, list) ->
                            val last = list.maxByOrNull { it.timestamp } ?: list.first()
                            val name = contacts.find { it.phoneNumber == number }?.name?.ifBlank { null }
                                ?: last.name?.ifBlank { null }
                                ?: number
                            ThreadSummary(
                                number = number,
                                name = name,
                                lastBody = last.body,
                                lastTimestamp = last.timestamp,
                                messages = list
                            )
                        }
                        .filter { thread ->
                            val query = searchQuery.trim()
                            query.isEmpty() ||
                            thread.number.contains(query, ignoreCase = true) ||
                            thread.name.contains(query, ignoreCase = true) ||
                            thread.messages.any { it.body.contains(query, ignoreCase = true) }
                        }
                        .let { threads ->
                            when (sortMode) {
                                MessageSortMode.NEWEST -> threads.sortedByDescending { it.lastTimestamp }
                                MessageSortMode.OLDEST -> threads.sortedBy { it.lastTimestamp }
                                MessageSortMode.NAME_ASC -> threads.sortedBy { it.name.lowercase() }
                                MessageSortMode.NAME_DESC -> threads.sortedByDescending { it.name.lowercase() }
                            }
                        }
                }
            }

            val todayThreads by remember(threads) {
                derivedStateOf { threads.filter { it.lastTimestamp.toMessageDayCategory() == MessageDayCategory.TODAY } }
            }
            val yesterdayThreads by remember(threads) {
                derivedStateOf { threads.filter { it.lastTimestamp.toMessageDayCategory() == MessageDayCategory.YESTERDAY } }
            }
            val olderThreads by remember(threads) {
                derivedStateOf { threads.filter { it.lastTimestamp.toMessageDayCategory() == MessageDayCategory.OLDER } }
            }

            LaunchedEffect(hasReadSms) {
                if (hasReadSms) {
                    viewModel.syncSms()
                }
            }

            LaunchedEffect(deleteProgress) {
                if (deleteProgress == null) {
                    selectedNumbers = emptySet()
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete ${selectedNumbers.size} conversation${if (selectedNumbers.size > 1) "s" else ""}?") },
                    text = { Text("This will permanently remove all messages from the selected number(s).") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteMessages(messages.filter { it.number in selectedNumbers })
                            showDeleteDialog = false
                        }) { Text("Delete", color = Color.Red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    }
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search messages") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search messages",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
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
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort"
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            MessageSortMode.values().forEach { mode ->
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

                if (inSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${selectedNumbers.size} selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(onClick = {
                                selectedNumbers = threads.map { it.number }.toSet()
                            }) { Text("Select all") }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red
                                )
                            }
                            IconButton(onClick = { selectedNumbers = emptySet() }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Cancel"
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    if (todayThreads.isNotEmpty()) {
                        item {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(todayThreads, key = { it.number }) { thread ->
                            ThreadListItem(
                                thread = thread,
                                inSelectionMode = inSelectionMode,
                                isSelected = thread.number in selectedNumbers,
                                onClick = {
                                    if (inSelectionMode) {
                                        selectedNumbers = if (thread.number in selectedNumbers) {
                                            selectedNumbers - thread.number
                                        } else {
                                            selectedNumbers + thread.number
                                        }
                                    } else {
                                        selectedThread = thread.number
                                    }
                                },
                                onLongClick = { selectedNumbers = selectedNumbers + thread.number }
                            )
                        }
                    }
                    if (yesterdayThreads.isNotEmpty()) {
                        item {
                            Text(
                                text = "Yesterday",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(yesterdayThreads, key = { it.number }) { thread ->
                            ThreadListItem(
                                thread = thread,
                                inSelectionMode = inSelectionMode,
                                isSelected = thread.number in selectedNumbers,
                                onClick = {
                                    if (inSelectionMode) {
                                        selectedNumbers = if (thread.number in selectedNumbers) {
                                            selectedNumbers - thread.number
                                        } else {
                                            selectedNumbers + thread.number
                                        }
                                    } else {
                                        selectedThread = thread.number
                                    }
                                },
                                onLongClick = { selectedNumbers = selectedNumbers + thread.number }
                            )
                        }
                    }
                    if (olderThreads.isNotEmpty()) {
                        item {
                            Text(
                                text = "Older",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(olderThreads, key = { it.number }) { thread ->
                            ThreadListItem(
                                thread = thread,
                                inSelectionMode = inSelectionMode,
                                isSelected = thread.number in selectedNumbers,
                                onClick = {
                                    if (inSelectionMode) {
                                        selectedNumbers = if (thread.number in selectedNumbers) {
                                            selectedNumbers - thread.number
                                        } else {
                                            selectedNumbers + thread.number
                                        }
                                    } else {
                                        selectedThread = thread.number
                                    }
                                },
                                onLongClick = { selectedNumbers = selectedNumbers + thread.number }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showNewMessage = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New SMS"
            )
        }

        selectedThread?.let { number ->
            MessageThreadScreen(
                number = number,
                viewModel = viewModel,
                hasPermission = hasPermission,
                requestPermissions = requestPermissions,
                onBack = { selectedThread = null },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showNewMessage) {
            NewMessageScreen(
                viewModel = viewModel,
                hasPermission = hasPermission,
                requestPermissions = requestPermissions,
                onBack = { showNewMessage = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadListItem(
    thread: ThreadSummary,
    inSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (inSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = thread.lastBody,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
        }
    }
}

private fun Long.messageIsToday(): Boolean = messageIsSameDay(System.currentTimeMillis())

private fun Long.messageIsYesterday(): Boolean = messageIsSameDay(System.currentTimeMillis() - 24 * 60 * 60 * 1000)

private fun Long.messageIsSameDay(other: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = this@messageIsSameDay }
    val c2 = Calendar.getInstance().apply { timeInMillis = other }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun Long.toMessageDayCategory(): MessageDayCategory = when {
    messageIsToday() -> MessageDayCategory.TODAY
    messageIsYesterday() -> MessageDayCategory.YESTERDAY
    else -> MessageDayCategory.OLDER
}
