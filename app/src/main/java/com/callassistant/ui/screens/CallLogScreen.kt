package com.callassistant.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.callassistant.permission.Permissions
import com.callassistant.ui.MainViewModel
import com.callassistant.ui.components.PermissionGuard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val category: DayCategory,
    val entries: List<CallLogEntry>,
    val id: String = "$number|${category.name}"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallLogScreen(
    viewModel: MainViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionGuard(
        permission = Permissions.callLog,
        hasPermission = hasPermission,
        requestPermissions = requestPermissions,
        modifier = modifier
    ) {
        val logs by viewModel.callLogs.collectAsStateWithLifecycle()
        var showBlockDialog by remember { mutableStateOf(false) }
        val hasCallLogPermission = hasPermission(Permissions.callLog.permission)
        var query by remember { mutableStateOf("") }
        var sortMode by remember { mutableStateOf(CallLogSortMode.NEWEST) }
        var selectedNumbers by remember { mutableStateOf(setOf<String>()) }
        val inSelectionMode = selectedNumbers.isNotEmpty()
        var expandedNumbers by remember { mutableStateOf(setOf<String>()) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        val allGroups by remember(logs, query, sortMode) {
            derivedStateOf {
                val filtered = if (query.isBlank()) logs else {
                    logs.filter {
                        (it.name ?: it.number).contains(query, ignoreCase = true) ||
                            it.number.contains(query, ignoreCase = true)
                    }
                }
                val groups = filtered
                    .groupBy { it.number to it.timestamp.toDayCategory() }
                    .map { (pair, entries) ->
                        val (number, category) = pair
                        CallLogGroup(
                            number = number,
                            name = entries.firstOrNull { it.name != null }?.name,
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

        val listState = rememberLazyListState()
        val pageSize = 20
        var olderLimit by remember { mutableStateOf(pageSize) }

        val todayGroups by remember(allGroups) { derivedStateOf { allGroups.filter { it.category == DayCategory.TODAY } } }
        val yesterdayGroups by remember(allGroups) { derivedStateOf { allGroups.filter { it.category == DayCategory.YESTERDAY } } }
        val olderGroups by remember(allGroups) { derivedStateOf { allGroups.filter { it.category == DayCategory.OLDER } } }
        val displayedGroups by remember(todayGroups, yesterdayGroups, olderGroups, olderLimit) { derivedStateOf { todayGroups + yesterdayGroups + olderGroups.take(olderLimit) } }

        val shouldLoadMore by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisible = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
                lastVisible > 0 && lastVisible >= totalItems - 2
            }
        }

        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore && olderLimit < olderGroups.size) {
                olderLimit = (olderLimit + pageSize).coerceAtMost(olderGroups.size)
            }
        }

        LaunchedEffect(hasCallLogPermission) {
            if (hasCallLogPermission) {
                viewModel.syncCallLogs()
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
                    }) { Text("Delete", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
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
                    }) { Text("Block", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search call log") },
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
                            CallLogSortMode.values().forEach { mode ->
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
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${selectedNumbers.size} selected", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selectedNumbers = displayedGroups.map { it.number }.toSet() }) {
                                Text("Select all")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                            IconButton(onClick = { showBlockDialog = true }) {
                                Icon(Icons.Filled.Block, contentDescription = "Block", tint = Color.Red)
                            }
                            IconButton(onClick = { selectedNumbers = emptySet() }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Cancel")
                            }
                        }
                    }
                }

                val onToggleSelection = { number: String ->
                    selectedNumbers = if (number in selectedNumbers) selectedNumbers - number else selectedNumbers + number
                }
                val onAddToSelection = { number: String ->
                    selectedNumbers = selectedNumbers + number
                }
                val onToggleExpand = { id: String ->
                    expandedNumbers = if (id in expandedNumbers) expandedNumbers - id else expandedNumbers + id
                }

                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    if (todayGroups.isNotEmpty()) {
                        item {
                            Text(
                                "Today",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(todayGroups, key = { it.id }) { group ->
                            CallLogGroupItem(
                                group = group,
                                inSelectionMode = inSelectionMode,
                                isSelected = group.number in selectedNumbers,
                                isExpanded = group.id in expandedNumbers,
                                onSelect = { onToggleSelection(group.number) },
                                onLongClick = { onAddToSelection(group.number) },
                                onToggleExpand = { onToggleExpand(group.id) },
                                hasPermission = hasPermission,
                                requestPermissions = requestPermissions
                            )
                        }
                    }
                    if (yesterdayGroups.isNotEmpty()) {
                        item {
                            Text(
                                "Yesterday",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(yesterdayGroups, key = { it.id }) { group ->
                            CallLogGroupItem(
                                group = group,
                                inSelectionMode = inSelectionMode,
                                isSelected = group.number in selectedNumbers,
                                isExpanded = group.id in expandedNumbers,
                                onSelect = { onToggleSelection(group.number) },
                                onLongClick = { onAddToSelection(group.number) },
                                onToggleExpand = { onToggleExpand(group.id) },
                                hasPermission = hasPermission,
                                requestPermissions = requestPermissions
                            )
                        }
                    }
                    if (olderGroups.isNotEmpty()) {
                        item {
                            Text(
                                "Older",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(olderGroups.take(olderLimit), key = { it.id }) { group ->
                            CallLogGroupItem(
                                group = group,
                                inSelectionMode = inSelectionMode,
                                isSelected = group.number in selectedNumbers,
                                isExpanded = group.id in expandedNumbers,
                                onSelect = { onToggleSelection(group.number) },
                                onLongClick = { onAddToSelection(group.number) },
                                onToggleExpand = { onToggleExpand(group.id) },
                                hasPermission = hasPermission,
                                requestPermissions = requestPermissions
                            )
                        }
                        if (olderLimit < olderGroups.size) {
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
            FloatingActionButton(
                onClick = { viewModel.syncCallLogs() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync call log")
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
    isExpanded: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit,
    onToggleExpand: () -> Unit,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val mostRecent = group.entries.first()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = if (isSelected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (inSelectionMode) {
                                onSelect()
                            } else if (group.entries.size > 1) {
                                onToggleExpand()
                            }
                        },
                        onLongClick = onLongClick
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (inSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelect() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    val displayName = group.name?.takeIf { it.isNotBlank() }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName ?: group.number,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (group.entries.size > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${group.entries.size}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (displayName != null) {
                        Text(
                            text = group.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (icon, tint) = when (mostRecent.type) {
                            CallType.INCOMING -> Icons.AutoMirrored.Filled.ArrowBack to MaterialTheme.colorScheme.primary
                            CallType.OUTGOING -> Icons.AutoMirrored.Filled.ArrowForward to Color(0xFF4CAF50)
                            CallType.MISSED -> Icons.Filled.Clear to Color.Red
                        }
                        Icon(imageVector = icon, contentDescription = mostRecent.type.name, tint = tint, modifier = Modifier.size(16.dp))
                        Text(
                            text = " ${mostRecent.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${formatDate(mostRecent.timestamp)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (mostRecent.blocked) {
                        Text(text = "Blocked", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (!inSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (group.entries.size > 1) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (hasPermission(Manifest.permission.CALL_PHONE)) {
                                        context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${group.number}")))
                                    } else requestPermissions()
                                },
                                modifier = Modifier.size(32.dp)
                            ) { Icon(Icons.Filled.Call, contentDescription = "Call") }
                            IconButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${group.number}"))) },
                                modifier = Modifier.size(32.dp)
                            ) { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS") }
                            AppActionButton("WA", Color(0xFF25D366)) { openWhatsApp(context, group.number) }
                            AppActionButton("IMO", Color(0xFF6C27D5)) { openImo(context, group.number) }
                        }
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider()
                group.entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (icon, tint) = when (entry.type) {
                            CallType.INCOMING -> Icons.AutoMirrored.Filled.ArrowBack to MaterialTheme.colorScheme.primary
                            CallType.OUTGOING -> Icons.AutoMirrored.Filled.ArrowForward to Color(0xFF4CAF50)
                            CallType.MISSED -> Icons.Filled.Clear to Color.Red
                        }
                        Icon(imageVector = icon, contentDescription = entry.type.name, tint = tint, modifier = Modifier.size(14.dp))
                        Text(
                            text = " ${entry.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${formatDate(entry.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        val dur = formatDuration(entry.duration)
                        if (dur.isNotEmpty()) {
                            Text(
                                text = dur,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index < group.entries.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(start = 32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppActionButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
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
