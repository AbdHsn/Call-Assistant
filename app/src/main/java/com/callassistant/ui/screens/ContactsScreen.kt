package com.callassistant.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.callassistant.data.entity.Contact
import com.callassistant.permission.Permissions
import com.callassistant.ui.MainViewModel
import com.callassistant.ui.components.PermissionGuard

private enum class ContactSortMode(val label: String) {
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    NEWEST("Newest"),
    OLDEST("Oldest"),
    MOST_USED("Most used")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    viewModel: MainViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionGuard(
        permission = Permissions.contacts,
        hasPermission = hasPermission,
        requestPermissions = requestPermissions,
        modifier = modifier
    ) {
        val contacts by viewModel.contacts.collectAsStateWithLifecycle()
        val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
        val hasContacts = hasPermission(Permissions.contacts.permission)
        var query by remember { mutableStateOf("") }
        var sortMode by remember { mutableStateOf(ContactSortMode.NAME_ASC) }

        val filteredContacts by remember(contacts, query, sortMode, callLogs) {
            derivedStateOf {
                val filtered = if (query.isBlank()) contacts else {
                    contacts.filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.phoneNumber.contains(query, ignoreCase = true)
                    }
                }
                when (sortMode) {
                    ContactSortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
                    ContactSortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
                    ContactSortMode.NEWEST -> filtered.sortedByDescending { it.id }
                    ContactSortMode.OLDEST -> filtered.sortedBy { it.id }
                    ContactSortMode.MOST_USED -> {
                        val counts = callLogs.groupBy {
                            it.number.replace(" ", "").replace("-", "")
                        }.mapValues { it.value.size }
                        filtered.sortedByDescending {
                            counts[it.phoneNumber.replace(" ", "").replace("-", "")] ?: 0
                        }
                    }
                }
            }
        }

        LaunchedEffect(hasContacts) {
            if (hasContacts) {
                viewModel.syncContacts()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 65.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search contacts") },
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
                            ContactSortMode.values().forEach { mode ->
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
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredContacts, key = { it.id }) { contact ->
                    val context = LocalContext.current
                    Card(
                        modifier = Modifier
                            .animateItemPlacement()
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
                                Text(text = contact.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (hasPermission(Manifest.permission.CALL_PHONE)) {
                                            context.startActivity(
                                                Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.phoneNumber}"))
                                            )
                                        } else {
                                            requestPermissions()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = "Call")
                                }
                                IconButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phoneNumber}"))
                                        )
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS")
                                }
                                AppActionButton(
                                    label = "WA",
                                    color = Color(0xFF25D366),
                                    onClick = { openWhatsApp(context, contact.phoneNumber) }
                                )
                                AppActionButton(
                                    label = "IMO",
                                    color = Color(0xFF6C27D5),
                                    onClick = { openImo(context, contact.phoneNumber) }
                                )
                            }
                        }
                    }
                }
            }
            }
            FloatingActionButton(
                onClick = { viewModel.syncContacts() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync contacts")
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

private fun openWhatsApp(context: android.content.Context, phoneNumber: String) {
    val digits = phoneNumber.filter { it.isDigit() }
    val uri = Uri.parse("https://wa.me/$digits")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.whatsapp")
    }
    startOrFallback(context, intent, "WhatsApp not installed")
}

private fun openImo(context: android.content.Context, phoneNumber: String) {
    val digits = phoneNumber.filter { it.isDigit() }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("imo://user/$digits"))
    startOrFallback(context, intent, "IMO not installed")
}

private fun startOrFallback(context: android.content.Context, intent: Intent, fallbackMessage: String) {
    try {
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            android.widget.Toast.makeText(context, fallbackMessage, android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (_: Exception) {
        android.widget.Toast.makeText(context, fallbackMessage, android.widget.Toast.LENGTH_SHORT).show()
    }
}
