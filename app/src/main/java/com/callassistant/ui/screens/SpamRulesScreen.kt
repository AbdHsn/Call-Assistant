package com.callassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.data.entity.BlockedNumber
import com.callassistant.data.entity.RuleType
import com.callassistant.data.entity.SpamRule
import com.callassistant.ui.SpamRulesViewModel
import com.callassistant.ui.theme.ErrorRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpamRulesScreen(
    viewModel: SpamRulesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rules = uiState.spamRules
    val blockedNumbers = uiState.blockedNumbers
    var selectedTab by remember { mutableStateOf(0) }
    var showRuleDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Rules") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Blocked Numbers") }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rules, key = { it.id }) { rule ->
                        RuleItem(rule = rule, onDelete = { viewModel.deleteSpamRule(rule) })
                    }
                }
                FloatingActionButton(
                    onClick = { showRuleDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add rule")
                }
            } else {
                if (blockedNumbers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "No blocked numbers yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(blockedNumbers, key = { it.id }) { blocked ->
                            BlockedNumberItem(
                                blocked = blocked,
                                onUnblock = { viewModel.unblockNumber(blocked.number) }
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showBlockDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Block number")
                }
            }
        }
    }

    if (showRuleDialog) {
        AddRuleDialog(
            onDismiss = { showRuleDialog = false },
            onConfirm = { pattern, type, label, blocking ->
                viewModel.addSpamRule(pattern, type, label, blocking)
                showRuleDialog = false
            }
        )
    }

    if (showBlockDialog) {
        AddBlockedNumberDialog(
            onDismiss = { showBlockDialog = false },
            onConfirm = { number, name, reason ->
                viewModel.blockNumber(number, reason.ifBlank { "Manually blocked" }, name.ifBlank { null })
                showBlockDialog = false
            }
        )
    }
}

@Composable
fun BlockedNumberItem(blocked: BlockedNumber, onUnblock: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = blocked.name?.takeIf { it.isNotBlank() } ?: blocked.number,
                    style = MaterialTheme.typography.titleMedium
                )
                if (blocked.name?.isNotBlank() == true) {
                    Text(text = blocked.number, style = MaterialTheme.typography.bodySmall)
                }
                if (blocked.reason.isNotBlank()) {
                    Text(text = blocked.reason, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = "Blocked " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(blocked.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (blocked.attemptCount > 0) {
                    Text(
                        text = "${blocked.attemptCount} call attempt${if (blocked.attemptCount > 1) "s" else ""}" +
                            (blocked.lastAttemptAt?.let {
                                ", last on " + SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it))
                            } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            TextButton(onClick = onUnblock) {
                Text("Unblock", color = ErrorRed)
            }
        }
    }
}

@Composable
fun AddBlockedNumberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Block a number", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(number.trim(), name.trim(), reason.trim()) },
                        modifier = Modifier.padding(start = 8.dp),
                        enabled = number.isNotBlank()
                    ) { Text("Block") }
                }
            }
        }
    }
}

@Composable
fun RuleItem(rule: SpamRule, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rule.label, style = MaterialTheme.typography.titleMedium)
                Text(text = rule.pattern, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${rule.type.name} · ${if (rule.isBlocking) "blocking" else "flag only"}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete rule")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, RuleType, String, Boolean) -> Unit
) {
    var pattern by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(RuleType.REGEX) }
    var expanded by remember { mutableStateOf(false) }
    var blocking by remember { mutableStateOf(true) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Add spam rule", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Pattern") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        RuleType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Blocking")
                    Switch(checked = blocking, onCheckedChange = { blocking = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(pattern, selectedType, label, blocking) },
                        modifier = Modifier.padding(start = 8.dp),
                        enabled = pattern.isNotBlank() && label.isNotBlank()
                    ) { Text("Save") }
                }
            }
        }
    }
}
