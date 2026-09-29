package com.callassistant.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import com.callassistant.permission.Permissions
import com.callassistant.ui.MessagesViewModel
import com.callassistant.ui.messageai.MessageAiViewModel
import com.callassistant.ui.components.MessageContextMenuDialog
import com.callassistant.ui.components.LinkifiedMessageText
import com.callassistant.ui.util.ResponsiveScreenContainer
import com.callassistant.ui.util.rememberListLayoutMetrics
import com.callassistant.ui.components.MessageEmptyState
import com.callassistant.ui.components.MessageThreadListSkeleton
import com.callassistant.ui.components.MessageSectionHeader
import com.callassistant.ui.components.formatMessagePreview
import com.callassistant.ui.components.formatThreadListTime
import com.callassistant.ui.components.PermissionGuard
import com.callassistant.ui.theme.ErrorRed
import com.callassistant.ui.util.rememberScrollPagination
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
    val photoUri: String?,
    val lastBody: String,
    val lastTimestamp: Long,
    val lastDirection: SmsDirection,
    val messages: List<SmsMessage> = emptyList()
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    aiViewModel: MessageAiViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    onOpenAiSettings: () -> Unit = {},
    onOverlayActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedThread by remember { mutableStateOf<String?>(null) }
    var showNewMessage by remember { mutableStateOf(false) }
    val pendingThread by viewModel.pendingThreadNumber.collectAsStateWithLifecycle()

    LaunchedEffect(showNewMessage, selectedThread) {
        onOverlayActiveChange(showNewMessage || selectedThread != null)
    }

    LaunchedEffect(pendingThread) {
        pendingThread?.let { number ->
            selectedThread = number
            viewModel.clearPendingThread()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PermissionGuard(
            permission = Permissions.readSms,
            hasPermission = hasPermission,
            requestPermissions = requestPermissions,
            modifier = Modifier.fillMaxSize()
        ) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val messages = uiState.smsMessages
            val contacts = uiState.contacts
            var searchQuery by remember { mutableStateOf("") }
            var sortMode by remember { mutableStateOf(MessageSortMode.NEWEST) }
            var selectedNumbers by remember { mutableStateOf(setOf<String>()) }
            val inSelectionMode = selectedNumbers.isNotEmpty()
            var showDeleteDialog by remember { mutableStateOf(false) }
            var contextMenuThread by remember { mutableStateOf<ThreadSummary?>(null) }
            val deleteProgress = uiState.deleteProgress
            val hasReadSms = hasPermission(Permissions.readSms.permission)

            val threads by remember(messages, contacts, searchQuery, sortMode) {
                derivedStateOf {
                    messages
                        .groupBy { it.number }
                        .map { (number, list) ->
                            val last = list.maxByOrNull { it.timestamp } ?: list.first()
                            val contact = contacts.find { it.phoneNumber == number }
                            val name = contact?.name?.ifBlank { null }
                                ?: last.name?.ifBlank { null }
                                ?: number
                            ThreadSummary(
                                number = number,
                                name = name,
                                photoUri = contact?.photoUri,
                                lastBody = last.body,
                                lastTimestamp = last.timestamp,
                                lastDirection = last.direction,
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
                        .let { sorted ->
                            when (sortMode) {
                                MessageSortMode.NEWEST -> sorted.sortedByDescending { it.lastTimestamp }
                                MessageSortMode.OLDEST -> sorted.sortedBy { it.lastTimestamp }
                                MessageSortMode.NAME_ASC -> sorted.sortedBy { it.name.lowercase() }
                                MessageSortMode.NAME_DESC -> sorted.sortedByDescending { it.name.lowercase() }
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

            val listState = rememberLazyListState()
            val olderLimit = rememberScrollPagination(
                totalItemCount = olderThreads.size,
                listState = listState,
                resetKey = searchQuery to sortMode
            )
            val displayedOlderThreads = olderThreads.take(olderLimit)
            val hasMoreOlderThreads = olderLimit < olderThreads.size

            LaunchedEffect(Unit, hasReadSms) {
                if (hasReadSms) {
                    viewModel.ensureSmsSynced()
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
                        }) { Text("Delete", color = ErrorRed) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    }
                )
            }

            val listMetrics = rememberListLayoutMetrics(inSelectionMode)
            val threadOpen = selectedThread != null || showNewMessage

            if (!threadOpen) {
            ResponsiveScreenContainer {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = listMetrics.horizontalPadding,
                            vertical = listMetrics.itemVerticalPadding
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search conversations") },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
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
                            MessageSortMode.entries.forEach { mode ->
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
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${selectedNumbers.size} selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
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
                                    tint = ErrorRed
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

                if (uiState.showMessagesSkeleton) {
                    MessageThreadListSkeleton(
                        modifier = Modifier.weight(1f)
                    )
                } else if (threads.isEmpty()) {
                    MessageEmptyState(
                        title = if (searchQuery.isBlank()) "No messages yet" else "No results",
                        subtitle = if (searchQuery.isBlank()) {
                            "Tap New message to start a conversation"
                        } else {
                            "Try a different name, number, or keyword"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = listMetrics.fabClearance)
                    ) {
                        if (todayThreads.isNotEmpty()) {
                            item { MessageSectionHeader("Today") }
                            items(todayThreads, key = { "today-${it.number}" }) { thread ->
                                ThreadListItem(
                                    thread = thread,
                                    inSelectionMode = inSelectionMode,
                                    isSelected = thread.number in selectedNumbers,
                                    onClick = threadClickHandler(
                                        thread, inSelectionMode, selectedNumbers,
                                        onSelect = { selectedNumbers = it },
                                        onOpen = { selectedThread = thread.number }
                                    ),
                                    onLongClick = { contextMenuThread = thread }
                                )
                            }
                        }
                        if (yesterdayThreads.isNotEmpty()) {
                            item { MessageSectionHeader("Yesterday") }
                            items(yesterdayThreads, key = { "yesterday-${it.number}" }) { thread ->
                                ThreadListItem(
                                    thread = thread,
                                    inSelectionMode = inSelectionMode,
                                    isSelected = thread.number in selectedNumbers,
                                    onClick = threadClickHandler(
                                        thread, inSelectionMode, selectedNumbers,
                                        onSelect = { selectedNumbers = it },
                                        onOpen = { selectedThread = thread.number }
                                    ),
                                    onLongClick = { contextMenuThread = thread }
                                )
                            }
                        }
                        if (olderThreads.isNotEmpty()) {
                            item { MessageSectionHeader("Older") }
                            items(displayedOlderThreads, key = { "older-${it.number}" }) { thread ->
                                ThreadListItem(
                                    thread = thread,
                                    inSelectionMode = inSelectionMode,
                                    isSelected = thread.number in selectedNumbers,
                                    onClick = threadClickHandler(
                                        thread, inSelectionMode, selectedNumbers,
                                        onSelect = { selectedNumbers = it },
                                        onOpen = { selectedThread = thread.number }
                                    ),
                                    onLongClick = { contextMenuThread = thread }
                                )
                            }
                            if (hasMoreOlderThreads) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            }

            contextMenuThread?.let { thread ->
                MessageContextMenuDialog(
                    messageText = thread.lastBody,
                    title = thread.name,
                    onDismiss = { contextMenuThread = null },
                    onSelect = {
                        selectedNumbers = selectedNumbers + thread.number
                    }
                )
            }
        }

        if (selectedThread == null && !showNewMessage) {
            ExtendedFloatingActionButton(
                onClick = { showNewMessage = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New message") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }

        selectedThread?.let { number ->
            MessageThreadScreen(
                number = number,
                viewModel = viewModel,
                aiViewModel = aiViewModel,
                hasPermission = hasPermission,
                requestPermissions = requestPermissions,
                onBack = { selectedThread = null },
                onOpenAiSettings = onOpenAiSettings,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showNewMessage) {
            NewMessageScreen(
                viewModel = viewModel,
                aiViewModel = aiViewModel,
                hasPermission = hasPermission,
                requestPermissions = requestPermissions,
                onBack = { showNewMessage = false },
                onOpenAiSettings = onOpenAiSettings,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun threadClickHandler(
    thread: ThreadSummary,
    inSelectionMode: Boolean,
    selectedNumbers: Set<String>,
    onSelect: (Set<String>) -> Unit,
    onOpen: () -> Unit
): () -> Unit = {
    if (inSelectionMode) {
        onSelect(
            if (thread.number in selectedNumbers) {
                selectedNumbers - thread.number
            } else {
                selectedNumbers + thread.number
            }
        )
    } else {
        onOpen()
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
    val metrics = rememberListLayoutMetrics(inSelectionMode)
    val preview = formatMessagePreview(thread.lastBody, thread.lastDirection)
    val background = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = metrics.horizontalPadding,
                    vertical = metrics.itemVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(metrics.rowGap)
        ) {
            if (inSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            }
            ContactAvatar(
                name = thread.name,
                photoUri = thread.photoUri,
                size = metrics.avatarSize
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatThreadListTime(thread.lastTimestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                LinkifiedMessageText(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    onNonLinkClick = onClick
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = metrics.dividerInset),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
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
