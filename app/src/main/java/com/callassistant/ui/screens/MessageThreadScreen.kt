package com.callassistant.ui.screens

import android.telephony.SmsManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.AlertDialog
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
import com.callassistant.ui.messageai.MessageAiViewModel
import com.callassistant.ai.model.AiContextScope
import com.callassistant.ai.model.MessageAiContext
import com.callassistant.ui.components.AiRecipientSummary
import com.callassistant.ui.components.AiSuggestionInlinePanel
import com.callassistant.ui.components.ChatBubble
import com.callassistant.ui.components.MessageContextMenuDialog
import com.callassistant.ui.components.MessageEmptyState
import com.callassistant.ui.components.NewMessageComposerBar
import com.callassistant.ui.components.CompactScreenHeader
import com.callassistant.ui.components.MessageThreadComposerBar
import com.callassistant.ui.components.MessageSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageThreadScreen(
    number: String,
    viewModel: MessagesViewModel,
    aiViewModel: MessageAiViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    onBack: () -> Unit,
    onOpenAiSettings: () -> Unit = {},
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
    var draftText by remember { mutableStateOf("") }
    var draftFromAi by remember { mutableStateOf(false) }
    var longPressedMessage by remember { mutableStateOf<SmsMessage?>(null) }
    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val insertEvent by aiViewModel.insertEvent.collectAsStateWithLifecycle()

    LaunchedEffect(insertEvent) {
        insertEvent?.let { text ->
            draftText = text
            draftFromAi = true
            aiViewModel.consumeInsertEvent()
        }
    }

    LaunchedEffect(threadMessages.size) {
        if (threadMessages.isNotEmpty()) {
            listState.scrollToItem(threadMessages.lastIndex)
        }
    }

    val messageAiContext = MessageAiContext(
        threadNumber = number,
        contactName = displayName,
        messages = threadMessages,
        scope = aiUiState.sheet.contextScope,
        anchorMessage = aiUiState.sheet.anchorMessage,
        draftText = draftText
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompactThreadHeader(
            displayName = displayName,
            number = number,
            photoUri = contact?.photoUri,
            onBack = onBack
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

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
                    contentPadding = PaddingValues(vertical = 8.dp)
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
                            showTail = showTail,
                            onLongClick = { longPressedMessage = message }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        AnimatedVisibility(
            visible = aiUiState.sheet.visible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            AiSuggestionInlinePanel(
                sheetState = aiUiState.sheet,
                modelStatus = aiUiState.modelStatus,
                inferenceReady = aiUiState.inferenceReady,
                messageContext = messageAiContext,
                showContextScope = true,
                onDismiss = { aiViewModel.dismissSheet() },
                onContextScopeChange = { scope ->
                    aiViewModel.setContextScope(scope)
                    aiViewModel.generateSuggestions(messageAiContext.copy(scope = scope))
                },
                onGenerate = { ctx ->
                    aiViewModel.generateSuggestions(
                        ctx.copy(
                            scope = aiUiState.sheet.contextScope,
                            anchorMessage = aiUiState.sheet.anchorMessage,
                            draftText = draftText
                        )
                    )
                },
                onSelectSuggestion = aiViewModel::selectSuggestion,
                onAskQueryChange = aiViewModel::setAskQuery,
                onRefineTone = aiViewModel::refineTone,
                onDownloadModel = { aiViewModel.downloadModel() },
                onNavigateToAiSettings = onOpenAiSettings
            )
        }

        ThreadComposer(
            value = draftText,
            onValueChange = {
                draftText = it
                draftFromAi = false
            },
            onOpenAi = { aiViewModel.openSheet() },
            showClearButton = draftFromAi,
            onClear = {
                draftText = ""
                draftFromAi = false
            },
            modifier = Modifier
                .imePadding()
                .navigationBarsPadding(),
            onSend = { body ->
                sendSmsMessage(
                    context = context,
                    number = number,
                    body = body,
                    hasPermission = hasPermission,
                    requestPermissions = requestPermissions,
                    onSaved = { viewModel.saveMessage(number, it, SmsDirection.OUT) }
                )
                draftText = ""
                draftFromAi = false
            }
        )
    }

    longPressedMessage?.let { message ->
        MessageContextMenuDialog(
            messageText = message.body,
            onDismiss = { longPressedMessage = null },
            onSuggestReply = {
                aiViewModel.openSheet(anchorMessage = message)
            }
        )
    }
}

@Composable
private fun CompactThreadHeader(
    displayName: String,
    number: String,
    photoUri: String?,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(22.dp)
                )
            }
            ContactAvatar(
                name = displayName,
                photoUri = photoUri,
                size = 36.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ThreadComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onOpenAi: () -> Unit,
    modifier: Modifier = Modifier,
    showClearButton: Boolean = false,
    onClear: (() -> Unit)? = null
) {
    MessageThreadComposerBar(
        value = value,
        onValueChange = onValueChange,
        onOpenAi = onOpenAi,
        showClearButton = showClearButton,
        onClear = onClear,
        onSend = {
            val trimmed = value.trim()
            if (trimmed.isNotBlank()) {
                onSend(trimmed)
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewMessageScreen(
    viewModel: MessagesViewModel,
    aiViewModel: MessageAiViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    onBack: () -> Unit,
    onOpenAiSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contacts = uiState.contacts
    val messages = uiState.smsMessages
    var query by remember { mutableStateOf("") }
    var selectedNumbers by remember { mutableStateOf(setOf<String>()) }
    var body by remember { mutableStateOf("") }
    var bodyFromAi by remember { mutableStateOf(false) }
    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val insertEvent by aiViewModel.insertEvent.collectAsStateWithLifecycle()

    LaunchedEffect(insertEvent) {
        insertEvent?.let { text ->
            body = text
            bodyFromAi = true
            aiViewModel.consumeInsertEvent()
        }
    }

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
        CompactScreenHeader(
            title = "New message",
            onBack = onBack
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

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

        val aiRecipientSummaries = remember(selectedNumbers, contactByNumber) {
            selectedNumbers.map { number ->
                AiRecipientSummary(
                    number = number,
                    displayName = contactByNumber[number]?.name?.ifBlank { null } ?: number,
                    photoUri = contactByNumber[number]?.photoUri
                )
            }
        }

        val messageAiContext = rememberNewMessageAiContext(
            selectedNumbers = selectedNumbers,
            query = query,
            manualPhoneCandidate = manualPhoneCandidate,
            body = body,
            messages = messages,
            contactByNumber = contactByNumber,
            contextScope = aiUiState.sheet.contextScope,
            anchorMessage = aiUiState.sheet.anchorMessage
        )

        AnimatedVisibility(
            visible = aiUiState.sheet.visible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            AiSuggestionInlinePanel(
                sheetState = aiUiState.sheet,
                modelStatus = aiUiState.modelStatus,
                inferenceReady = aiUiState.inferenceReady,
                messageContext = messageAiContext,
                recipients = aiRecipientSummaries,
                onDismiss = { aiViewModel.dismissSheet() },
                onContextScopeChange = { scope ->
                    aiViewModel.setContextScope(scope)
                    aiViewModel.generateSuggestions(messageAiContext.copy(scope = scope))
                },
                onGenerate = { ctx ->
                    aiViewModel.generateSuggestions(
                        ctx.copy(
                            scope = aiUiState.sheet.contextScope,
                            anchorMessage = aiUiState.sheet.anchorMessage,
                            draftText = body
                        )
                    )
                },
                onSelectSuggestion = aiViewModel::selectSuggestion,
                onAskQueryChange = aiViewModel::setAskQuery,
                onRefineTone = aiViewModel::refineTone,
                onDownloadModel = { aiViewModel.downloadModel() },
                onNavigateToAiSettings = onOpenAiSettings
            )
        }

        NewMessageComposerBar(
            value = body,
            onValueChange = {
                body = it
                bodyFromAi = false
            },
            placeholder = if (selectedNumbers.isEmpty()) {
                "Type your message… (add recipients to send)"
            } else {
                "Type your message…"
            },
            onOpenAi = { aiViewModel.openSheet() },
            showClearButton = bodyFromAi,
            onClear = {
                body = ""
                bodyFromAi = false
            },
            sendEnabled = selectedNumbers.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            onSend = {
                val trimmed = body.trim()
                if (selectedNumbers.isEmpty() || trimmed.isBlank()) return@NewMessageComposerBar
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
            }
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = if (selectedNumbers.isEmpty()) {
                Alignment.CenterVertically
            } else {
                Alignment.Top
            },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "To",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (selectedNumbers.isNotEmpty()) {
                    Modifier.padding(top = 10.dp)
                } else {
                    Modifier
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                if (selectedNumbers.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
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

@Composable
private fun rememberNewMessageAiContext(
    selectedNumbers: Set<String>,
    query: String,
    manualPhoneCandidate: String?,
    body: String,
    messages: List<SmsMessage>,
    contactByNumber: Map<String, Contact>,
    contextScope: AiContextScope,
    anchorMessage: SmsMessage?
): MessageAiContext {
    val primaryNumber = selectedNumbers.firstOrNull()
        ?: manualPhoneCandidate
        ?: query.filter { it.isDigit() || it == '+' }.takeIf { it.length >= 7 }

    val contactName = primaryNumber?.let { contactByNumber[it]?.name?.ifBlank { null } }
        ?: query.trim().takeIf { text ->
            text.isNotBlank() && text.any { !it.isDigit() && it !in "+-() " }
        }

    val threadMessages = primaryNumber?.let { number ->
        messages.filter { it.number == number }.sortedBy { it.timestamp }
    }.orEmpty()

    return remember(
        primaryNumber,
        contactName,
        threadMessages.size,
        threadMessages.lastOrNull()?.id,
        body,
        contextScope,
        anchorMessage?.id
    ) {
        MessageAiContext(
            threadNumber = primaryNumber ?: "new-message",
            contactName = contactName,
            messages = threadMessages,
            scope = contextScope,
            anchorMessage = anchorMessage,
            draftText = body
        )
    }
}

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
