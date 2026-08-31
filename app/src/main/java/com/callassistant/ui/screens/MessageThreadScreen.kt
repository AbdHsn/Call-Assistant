package com.callassistant.ui.screens

import android.telephony.SmsManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import com.callassistant.permission.Permissions
import com.callassistant.ui.MessagesViewModel
import com.callassistant.ui.components.ChatBubble
import com.callassistant.ui.components.MessageEmptyState
import com.callassistant.ui.components.MessageComposerBar
import com.callassistant.ui.components.MessageThreadComposerBar
import com.callassistant.ui.components.MessageSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageThreadScreen(
    number: String,
    viewModel: MessagesViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = uiState.smsMessages
    val contacts = uiState.contacts
    val contact = contacts.find { it.phoneNumber == number }
    val displayName = contact?.name?.ifBlank { null } ?: number

    val threadMessages by remember(messages, number) {
        derivedStateOf {
            messages
                .filter { it.number == number }
                .sortedBy { it.timestamp }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(threadMessages.size) {
        if (threadMessages.isNotEmpty()) {
            listState.scrollToItem(threadMessages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContactAvatar(
                        name = displayName,
                        photoUri = contact?.photoUri,
                        size = 40.dp
                    )
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
        ) {
            if (threadMessages.isEmpty()) {
                MessageEmptyState(
                    title = "No messages",
                    subtitle = "Send a message to start the conversation",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    items(
                        items = threadMessages,
                        key = { it.id }
                    ) { message ->
                        val index = threadMessages.indexOf(message)
                        val prev = threadMessages.getOrNull(index - 1)
                        val isOutgoing = message.direction == SmsDirection.OUT
                        val showTail = prev == null || prev.direction != message.direction
                        ChatBubble(
                            message = message,
                            isOutgoing = isOutgoing,
                            showTail = showTail
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        ThreadComposer(
            onSend = { body ->
                sendSmsMessage(
                    context = context,
                    number = number,
                    body = body,
                    hasPermission = hasPermission,
                    requestPermissions = requestPermissions,
                    onSaved = { viewModel.saveMessage(number, it, SmsDirection.OUT) }
                )
            }
        )
    }
}

@Composable
private fun ThreadComposer(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    MessageThreadComposerBar(
        value = text,
        onValueChange = { text = it },
        onSend = {
            val trimmed = text.trim()
            if (trimmed.isNotBlank()) {
                onSend(trimmed)
                text = ""
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewMessageScreen(
    viewModel: MessagesViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contacts = uiState.contacts
    val messages = uiState.smsMessages
    var query by remember { mutableStateOf("") }
    var selectedNumbers by remember { mutableStateOf(setOf<String>()) }
    var body by remember { mutableStateOf("") }

    val contactByNumber = remember(contacts) {
        contacts.associateBy { it.phoneNumber }
    }

    val recentThreads by remember(messages, contacts) {
        derivedStateOf {
            messages
                .groupBy { it.number }
                .mapNotNull { (number, list) ->
                    val last = list.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                    val contact = contacts.find { it.phoneNumber == number }
                    val name = contact?.name?.ifBlank { null }
                        ?: last.name?.ifBlank { null }
                        ?: number
                    RecentThread(
                        number = number,
                        name = name,
                        photoUri = contact?.photoUri,
                        lastTimestamp = last.timestamp
                    )
                }
                .sortedByDescending { it.lastTimestamp }
                .take(8)
        }
    }

    val filteredContacts by remember(contacts, query) {
        derivedStateOf {
            val q = query.trim()
            if (q.isBlank()) {
                contacts.sortedBy { it.name.lowercase() }
            } else {
                contacts.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.phoneNumber.contains(q, ignoreCase = true)
                }
            }
        }
    }

    val manualPhoneCandidate = remember(query, filteredContacts) {
        val q = query.trim()
        if (q.isBlank()) return@remember null
        val looksLikePhone = q.any { it.isDigit() } &&
            q.all { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' }
        if (!looksLikePhone) return@remember null
        val normalized = q.filter { it.isDigit() || it == '+' }
        if (normalized.isBlank()) return@remember null
        val alreadyContact = contacts.any { it.phoneNumber.contains(normalized.takeLast(7)) }
        if (alreadyContact && filteredContacts.isNotEmpty()) return@remember null
        normalized
    }

    fun toggleNumber(number: String) {
        selectedNumbers = if (number in selectedNumbers) {
            selectedNumbers - number
        } else {
            selectedNumbers + number
        }
        query = ""
    }

    fun displayNameFor(number: String): String {
        return contactByNumber[number]?.name?.ifBlank { null } ?: number
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                Text(
                    text = "New message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        RecipientField(
            selectedNumbers = selectedNumbers,
            query = query,
            onQueryChange = { query = it },
            displayNameFor = ::displayNameFor,
            photoUriFor = { contactByNumber[it]?.photoUri },
            onRemove = { selectedNumbers = selectedNumbers - it }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (query.isBlank() && recentThreads.isNotEmpty()) {
                item { MessageSectionHeader("Recent") }
                items(recentThreads, key = { "recent-${it.number}" }) { thread ->
                    RecipientRow(
                        name = thread.name,
                        subtitle = thread.number,
                        photoUri = thread.photoUri,
                        isSelected = thread.number in selectedNumbers,
                        onClick = { toggleNumber(thread.number) }
                    )
                }
            }

            if (manualPhoneCandidate != null) {
                item { MessageSectionHeader("Send to number") }
                item {
                    RecipientRow(
                        name = manualPhoneCandidate,
                        subtitle = "Tap to add recipient",
                        photoUri = null,
                        isSelected = manualPhoneCandidate in selectedNumbers,
                        leadingIcon = Icons.Filled.Phone,
                        onClick = { toggleNumber(manualPhoneCandidate) }
                    )
                }
            }

            item {
                MessageSectionHeader(
                    if (query.isBlank()) "Contacts" else "Results"
                )
            }

            if (filteredContacts.isEmpty() && manualPhoneCandidate == null) {
                item {
                    MessageEmptyState(
                        title = if (query.isBlank()) "No contacts" else "No matches",
                        subtitle = if (query.isBlank()) {
                            "Add contacts or type a phone number above"
                        } else {
                            "Try another name or enter a phone number"
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(filteredContacts, key = { it.id }) { contact ->
                    RecipientRow(
                        name = contact.name.ifBlank { contact.phoneNumber },
                        subtitle = contact.phoneNumber,
                        photoUri = contact.photoUri,
                        isSelected = contact.phoneNumber in selectedNumbers,
                        onClick = { toggleNumber(contact.phoneNumber) }
                    )
                }
            }
        }

        MessageComposerBar(
            value = body,
            onValueChange = { body = it },
            placeholder = if (selectedNumbers.isEmpty()) "Add recipients to send" else "Type your message…",
            minLines = 4,
            maxLines = 8,
            sendEnabled = selectedNumbers.isNotEmpty(),
            onSend = {
                val trimmed = body.trim()
                if (selectedNumbers.isEmpty() || trimmed.isBlank()) return@MessageComposerBar
                selectedNumbers.forEach { number ->
                    sendSmsMessage(
                        context = context,
                        number = number,
                        body = trimmed,
                        hasPermission = hasPermission,
                        requestPermissions = requestPermissions,
                        onSaved = { viewModel.saveMessage(number, it, SmsDirection.OUT) },
                        showToast = false
                    )
                }
                android.widget.Toast.makeText(
                    context,
                    "Message sent to ${selectedNumbers.size} recipient(s)",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipientField(
    selectedNumbers: Set<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    displayNameFor: (String) -> String,
    photoUriFor: (String) -> String?,
    onRemove: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "To",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                if (selectedNumbers.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        selectedNumbers.forEach { number ->
                            InputChip(
                                selected = true,
                                onClick = { onRemove(number) },
                                label = {
                                    Text(
                                        text = displayNameFor(number),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    ContactAvatar(
                                        name = displayNameFor(number),
                                        photoUri = photoUriFor(number),
                                        size = 24.dp
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Clear,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            if (selectedNumbers.isEmpty()) "Name or phone number" else "Add more"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RecipientRow(
    name: String,
    subtitle: String,
    photoUri: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val background = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (leadingIcon != null) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {
                ContactAvatar(name = name, photoUri = photoUri, size = 52.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSelected) {
                Text(
                    text = "Added",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 82.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

private data class RecentThread(
    val number: String,
    val name: String,
    val photoUri: String?,
    val lastTimestamp: Long
)

private fun sendSmsMessage(
    context: android.content.Context,
    number: String,
    body: String,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    onSaved: (String) -> Unit,
    showToast: Boolean = true
) {
    if (!hasPermission(Permissions.sendSms.permission)) {
        requestPermissions()
        android.widget.Toast.makeText(
            context,
            "SMS permission required to send messages",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        return
    }
    try {
        val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        smsManager.sendTextMessage(number, null, body, null, null)
        onSaved(body)
        if (showToast) {
            android.widget.Toast.makeText(context, "Message sent", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Failed to send: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
