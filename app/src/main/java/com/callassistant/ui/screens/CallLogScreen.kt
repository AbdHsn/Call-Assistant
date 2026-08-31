package com.callassistant.ui.screens

import android.Manifest
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog.Calls
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.CallType
import com.callassistant.data.entity.Contact
import com.callassistant.ui.theme.ErrorRed
import com.callassistant.ui.theme.SuccessGreen
import com.callassistant.permission.Permissions
import com.callassistant.ui.phonebook.PhoneBookViewModel
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.ui.text.style.TextOverflow
import com.callassistant.ui.components.AddContactDialog
import com.callassistant.ui.components.MessageEmptyState
import com.callassistant.ui.components.PhoneBookListSkeleton
import com.callassistant.ui.components.MessageSectionHeader
import com.callassistant.ui.components.PermissionGuard
import com.callassistant.ui.components.PhoneBookListDivider
import com.callassistant.ui.components.PhoneBookSearchRow
import com.callassistant.ui.components.PhoneBookSelectionBar
import com.callassistant.ui.components.PhoneBookTonalActionButton
import com.callassistant.ui.components.formatThreadListTime
import com.callassistant.ui.theme.MessageBlue
import com.callassistant.ui.util.rememberScrollPagination
import com.callassistant.util.PhoneNumberNormalizer
import com.callassistant.util.telCallUri
import java.util.Calendar

private enum class CallLogSortMode(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A")
}

private enum class DayCategory { TODAY, YESTERDAY, OLDER }

private data class CallLogGroup(
    val number: String,
    val name: String?,
    val photoUri: String?,
    val category: DayCategory,
    val entries: List<CallLogEntry>,
    val id: String = "$number|${category.name}"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallLogScreen(
    viewModel: PhoneBookViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    onOpenMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionGuard(
        permission = Permissions.callLog,
        hasPermission = hasPermission,
        requestPermissions = requestPermissions,
        modifier = modifier
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val logs = uiState.callLogs
        val contacts = uiState.contacts
        val context = LocalContext.current
        var showBlockDialog by remember { mutableStateOf(false) }
        val hasCallLogPermission = hasPermission(Permissions.callLog.permission)
        var query by remember { mutableStateOf("") }
        var sortMode by remember { mutableStateOf(CallLogSortMode.NEWEST) }
        var selectedNumbers by remember { mutableStateOf(setOf<String>()) }
        val inSelectionMode = selectedNumbers.isNotEmpty()
        var showDeleteDialog by remember { mutableStateOf(false) }
        var editingContact by remember { mutableStateOf<Contact?>(null) }
        var selectedDetailNumber by remember { mutableStateOf<String?>(null) }

        val allGroups by remember(logs, query, sortMode, contacts) {
            derivedStateOf {
                val filtered = if (query.isBlank()) logs else {
                    logs.filter {
                        val contact = contacts.find { c -> c.phoneNumber == it.number }
                        val displayName = contact?.name?.takeIf { it.isNotBlank() } ?: it.name ?: it.number
                        displayName.contains(query, ignoreCase = true) ||
                            it.number.contains(query, ignoreCase = true)
                    }
                }
                val groups = filtered
                    .groupBy { it.number to it.timestamp.toDayCategory() }
                    .map { (pair, entries) ->
                        val (number, category) = pair
                        val contactName = contacts.find { it.phoneNumber == number }?.name?.takeIf { it.isNotBlank() }
                        CallLogGroup(
                            number = number,
                            name = entries.firstOrNull { it.name?.isNotBlank() == true }?.name ?: contactName,
                            photoUri = contacts.find { it.phoneNumber == number }?.photoUri,
                            category = category,
                            entries = entries.sortedByDescending { it.timestamp }
                        )
                    }
                when (sortMode) {
                    CallLogSortMode.NEWEST -> groups.sortedByDescending { it.entries.first().timestamp }
                    CallLogSortMode.OLDEST -> groups.sortedBy { it.entries.first().timestamp }
                    CallLogSortMode.NAME_ASC -> groups.sortedBy { (it.name ?: it.number).lowercase() }
                    CallLogSortMode.NAME_DESC -> groups.sortedByDescending { (it.name ?: it.number).lowercase() }
                }
            }
        }

        val todayGroups by remember(allGroups) { derivedStateOf { allGroups.filter { it.category == DayCategory.TODAY } } }
        val yesterdayGroups by remember(allGroups) { derivedStateOf { allGroups.filter { it.category == DayCategory.YESTERDAY } } }
        val olderGroups by remember(allGroups) { derivedStateOf { allGroups.filter { it.category == DayCategory.OLDER } } }

        val listState = rememberLazyListState()
        val olderLimit = rememberScrollPagination(
            totalItemCount = olderGroups.size,
            listState = listState,
            resetKey = query to sortMode
        )
        val displayedOlderGroups = olderGroups.take(olderLimit)
        val hasMoreOlderGroups = olderLimit < olderGroups.size

        LaunchedEffect(hasCallLogPermission) {
            if (hasCallLogPermission) {
                viewModel.ensureCallLogsSynced()
            }
        }

        DisposableEffect(hasCallLogPermission) {
            var observer: ContentObserver? = null
            if (hasCallLogPermission) {
                observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        viewModel.syncCallLogs()
                    }
                }
                context.contentResolver.registerContentObserver(Calls.CONTENT_URI, true, observer)
            }
            onDispose {
                observer?.let { context.contentResolver.unregisterContentObserver(it) }
            }
        }

        if (showDeleteDialog) {
            val toDelete = logs.filter { it.number in selectedNumbers }
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete history for ${selectedNumbers.size} contact${if (selectedNumbers.size > 1) "s" else ""}?") },
                text = { Text("This will permanently remove all ${toDelete.size} call log entries.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteCallLogs(toDelete)
                        selectedNumbers = emptySet()
                        showDeleteDialog = false
                    }) { Text("Delete", color = ErrorRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (editingContact != null) {
            AddContactDialog(
                contact = editingContact,
                onConfirm = { contact ->
                    viewModel.saveContact(contact)
                    editingContact = null
                },
                onDismiss = { editingContact = null }
            )
        }

        if (showBlockDialog) {
            val groupsToBlock = allGroups.filter { it.number in selectedNumbers }.distinctBy { it.number }
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title = { Text("Block ${groupsToBlock.size} number${if (groupsToBlock.size > 1) "s" else ""}?") },
                text = { Text("You will no longer receive calls or messages from these numbers.") },
                confirmButton = {
                    TextButton(onClick = {
                        groupsToBlock.forEach { viewModel.blockNumber(it.number, name = it.name) }
                        selectedNumbers = emptySet()
                        showBlockDialog = false
                    }) { Text("Block", color = ErrorRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                PhoneBookSearchRow(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search call log",
                    sortOptions = CallLogSortMode.entries.map { it to it.label },
                    onSortSelected = { selected ->
                        sortMode = selected as CallLogSortMode
                    }
                )

                if (inSelectionMode) {
                    PhoneBookSelectionBar(
                        selectedCount = selectedNumbers.size,
                        onSelectAll = {
                            selectedNumbers = (todayGroups + yesterdayGroups + displayedOlderGroups)
                                .map { it.number }
                                .toSet()
                        },
                        onDelete = { showDeleteDialog = true },
                        onBlock = { showBlockDialog = true },
                        onCancel = { selectedNumbers = emptySet() }
                    )
                }

                val onOpenDetail = { number: String ->
                    selectedDetailNumber = number
                }
                val onToggleSelection = { number: String ->
                    selectedNumbers = if (number in selectedNumbers) selectedNumbers - number else selectedNumbers + number
                }
                val onAddToSelection = { number: String ->
                    selectedNumbers = selectedNumbers + number
                }

                if (uiState.showCallLogsSkeleton) {
                    PhoneBookListSkeleton(
                        modifier = Modifier.weight(1f),
                        showTrailingActions = true
                    )
                } else if (allGroups.isEmpty()) {
                    MessageEmptyState(
                        title = if (query.isBlank()) "No call history" else "No results",
                        subtitle = if (query.isBlank()) {
                            "Your recent calls will appear here"
                        } else {
                            "Try a different name or phone number"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        if (todayGroups.isNotEmpty()) {
                            item { MessageSectionHeader("Today") }
                            items(todayGroups, key = { it.id }) { group ->
                                CallLogGroupItem(
                                    group = group,
                                    inSelectionMode = inSelectionMode,
                                    isSelected = group.number in selectedNumbers,
                                    onSelect = { onToggleSelection(group.number) },
                                    onLongClick = { onAddToSelection(group.number) },
                                    onOpenDetail = { onOpenDetail(group.number) },
                                    onOpenMessage = onOpenMessage,
                                    hasPermission = hasPermission,
                                    requestPermissions = requestPermissions,
                                    onAddAsContact = { editingContact = Contact(name = "", phoneNumber = group.number) }
                                )
                            }
                        }
                        if (yesterdayGroups.isNotEmpty()) {
                            item { MessageSectionHeader("Yesterday") }
                            items(yesterdayGroups, key = { it.id }) { group ->
                                CallLogGroupItem(
                                    group = group,
                                    inSelectionMode = inSelectionMode,
                                    isSelected = group.number in selectedNumbers,
                                    onSelect = { onToggleSelection(group.number) },
                                    onLongClick = { onAddToSelection(group.number) },
                                    onOpenDetail = { onOpenDetail(group.number) },
                                    onOpenMessage = onOpenMessage,
                                    hasPermission = hasPermission,
                                    requestPermissions = requestPermissions,
                                    onAddAsContact = { editingContact = Contact(name = "", phoneNumber = group.number) }
                                )
                            }
                        }
                        if (olderGroups.isNotEmpty()) {
                            item { MessageSectionHeader("Older") }
                            items(displayedOlderGroups, key = { it.id }) { group ->
                                CallLogGroupItem(
                                    group = group,
                                    inSelectionMode = inSelectionMode,
                                    isSelected = group.number in selectedNumbers,
                                    onSelect = { onToggleSelection(group.number) },
                                    onLongClick = { onAddToSelection(group.number) },
                                    onOpenDetail = { onOpenDetail(group.number) },
                                    onOpenMessage = onOpenMessage,
                                    hasPermission = hasPermission,
                                    requestPermissions = requestPermissions,
                                    onAddAsContact = { editingContact = Contact(name = "", phoneNumber = group.number) }
                                )
                            }
                            if (hasMoreOlderGroups) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            ExtendedFloatingActionButton(
                onClick = { viewModel.syncCallLogs() },
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                text = { Text("Sync") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )

            selectedDetailNumber?.let { number ->
                val matchedContact = contacts.find { PhoneNumberNormalizer.matches(it.phoneNumber, number) }
                val relatedCallLogs = logs.filter { PhoneNumberNormalizer.matches(it.number, number) }
                ContactDetailScreen(
                    phoneNumber = number,
                    contact = matchedContact,
                    callLogs = relatedCallLogs,
                    onBack = { selectedDetailNumber = null },
                    onSaveContact = viewModel::saveContact,
                    onDeleteContact = viewModel::deleteContact,
                    onDeleteCallLogs = viewModel::deleteCallLogs,
                    onBlockNumber = { blockedNumber, name ->
                        viewModel.blockNumber(blockedNumber, name = name)
                    },
                    onOpenMessage = { number ->
                        selectedDetailNumber = null
                        onOpenMessage(number)
                    },
                    hasPermission = hasPermission,
                    requestPermissions = requestPermissions,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallLogGroupItem(
    group: CallLogGroup,
    inSelectionMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenMessage: (String) -> Unit,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    onAddAsContact: () -> Unit
) {
    val context = LocalContext.current
    val mostRecent = group.entries.first()
    val displayName = group.name?.takeIf { it.isNotBlank() }
    val background = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val dividerInset = if (inSelectionMode) 16.dp else 82.dp
    val duration = formatDuration(mostRecent.duration)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (inSelectionMode) onSelect() else onOpenDetail()
                    },
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (inSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            ContactAvatar(
                name = displayName ?: group.number,
                photoUri = group.photoUri,
                size = 52.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayName ?: group.number,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (displayName != null) {
                    Text(
                        text = group.number,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CallTypeIndicator(type = mostRecent.type)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${callTypeLabel(mostRecent.type)}, ${formatThreadListTime(mostRecent.timestamp)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (mostRecent.blocked) {
                        Text(
                            text = " · Blocked",
                            style = MaterialTheme.typography.labelMedium,
                            color = ErrorRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!inSelectionMode) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PhoneBookTonalActionButton(
                            icon = Icons.Filled.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                if (hasPermission(Manifest.permission.CALL_PHONE)) {
                                    context.startActivity(Intent(Intent.ACTION_CALL, telCallUri(group.number)))
                                } else {
                                    requestPermissions()
                                }
                            }
                        )
                        PhoneBookTonalActionButton(
                            icon = Icons.AutoMirrored.Filled.Message,
                            contentDescription = "Message",
                            tint = MessageBlue,
                            onClick = { onOpenMessage(group.number) }
                        )
                        Box {
                            var menuExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("WhatsApp") },
                                    onClick = {
                                        menuExpanded = false
                                        openWhatsApp(context, group.number)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("IMO") },
                                    onClick = {
                                        menuExpanded = false
                                        openImo(context, group.number)
                                    }
                                )
                                if (group.name.isNullOrBlank()) {
                                    DropdownMenuItem(
                                        text = { Text("Add contact") },
                                        onClick = {
                                            menuExpanded = false
                                            onAddAsContact()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (duration.isNotEmpty()) {
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        PhoneBookListDivider(leadingInset = dividerInset)
    }
}

@Composable
private fun CallTypeIndicator(
    type: CallType,
    size: androidx.compose.ui.unit.Dp = 16.dp
) {
    val (icon, tint) = when (type) {
        CallType.INCOMING -> Icons.AutoMirrored.Filled.ArrowBack to MaterialTheme.colorScheme.primary
        CallType.OUTGOING -> Icons.AutoMirrored.Filled.ArrowForward to SuccessGreen
        CallType.MISSED -> Icons.Filled.Clear to ErrorRed
    }
    Icon(
        imageVector = icon,
        contentDescription = type.name,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

private fun callTypeLabel(type: CallType): String = when (type) {
    CallType.INCOMING -> "Incoming"
    CallType.OUTGOING -> "Outgoing"
    CallType.MISSED -> "Missed"
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

private fun Long.isToday(): Boolean = isSameDay(System.currentTimeMillis())

private fun Long.isYesterday(): Boolean = isSameDay(System.currentTimeMillis() - 24 * 60 * 60 * 1000)

private fun Long.isSameDay(other: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = this@isSameDay }
    val c2 = Calendar.getInstance().apply { timeInMillis = other }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun Long.toDayCategory(): DayCategory = when {
    isToday() -> DayCategory.TODAY
    isYesterday() -> DayCategory.YESTERDAY
    else -> DayCategory.OLDER
}
