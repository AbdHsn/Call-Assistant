package com.callassistant.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import com.callassistant.ui.theme.ErrorRed
import com.callassistant.ui.theme.MessageBlue
import com.callassistant.util.copyTextToClipboard
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CompactScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MessageComposerBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Message",
    minLines: Int = 1,
    maxLines: Int = 5,
    sendEnabled: Boolean = true,
    sendButtonLabel: String = "Send message",
    onOpenAi: (() -> Unit)? = null,
    showClearButton: Boolean = false,
    onClear: (() -> Unit)? = null,
    forceCompactLayout: Boolean = false
) {
    val canSend = sendEnabled && value.isNotBlank()
    val showClear = showClearButton && value.isNotBlank() && onClear != null

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        if (forceCompactLayout) {
            CompactMessageComposer(
                value = value,
                onValueChange = onValueChange,
                onSend = onSend,
                onOpenAi = onOpenAi,
                onClear = onClear,
                showClear = showClear,
                placeholder = placeholder,
                minLines = minLines,
                maxLines = maxLines,
                canSend = canSend
            )
        } else {
            ExpandedMessageComposer(
                value = value,
                onValueChange = onValueChange,
                onSend = onSend,
                onOpenAi = onOpenAi,
                onClear = onClear,
                showClear = showClear,
                placeholder = placeholder,
                minLines = minLines,
                maxLines = maxLines,
                canSend = canSend,
                sendButtonLabel = sendButtonLabel
            )
        }
    }
}

/** Compact composer for the new-message screen — keeps more room for contact selection. */
@Composable
fun NewMessageComposerBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenAi: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "Type a message…",
    sendEnabled: Boolean = true,
    showClearButton: Boolean = false,
    onClear: (() -> Unit)? = null
) {
    MessageComposerBar(
        value = value,
        onValueChange = onValueChange,
        onSend = onSend,
        onOpenAi = onOpenAi,
        onClear = onClear,
        showClearButton = showClearButton,
        placeholder = placeholder,
        minLines = 2,
        maxLines = 4,
        sendEnabled = sendEnabled,
        sendButtonLabel = "Send",
        forceCompactLayout = true,
        modifier = modifier
    )
}

/** Multi-line composer for an open message thread. */
@Composable
fun MessageThreadComposerBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenAi: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "Type a message…",
    sendEnabled: Boolean = true,
    showClearButton: Boolean = false,
    onClear: (() -> Unit)? = null
) {
    MessageComposerBar(
        value = value,
        onValueChange = onValueChange,
        onSend = onSend,
        onOpenAi = onOpenAi,
        onClear = onClear,
        showClearButton = showClearButton,
        placeholder = placeholder,
        minLines = 2,
        maxLines = 6,
        sendEnabled = sendEnabled,
        sendButtonLabel = "Send",
        forceCompactLayout = true,
        modifier = modifier
    )
}

@Composable
private fun ExpandedMessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenAi: (() -> Unit)?,
    onClear: (() -> Unit)?,
    showClear: Boolean,
    placeholder: String,
    minLines: Int,
    maxLines: Int,
    canSend: Boolean,
    sendButtonLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onOpenAi != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.material3.TextButton(onClick = onOpenAi) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MessageBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI suggest", color = MessageBlue)
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            ),
            shape = RoundedCornerShape(16.dp),
            minLines = minLines,
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = (minLines * 26).dp)
                .heightIn(min = (minLines * 26).dp, max = (maxLines * 26).dp)
        )

        if (showClear && onClear != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }
                Button(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MessageBlue,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sendButtonLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MessageBlue,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = sendButtonLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CompactMessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenAi: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    showClear: Boolean = false,
    placeholder: String,
    minLines: Int,
    maxLines: Int,
    canSend: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onOpenAi != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MessageBlue.copy(alpha = 0.12f))
                    .clickable(onClick = onOpenAi),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI suggest",
                    tint = MessageBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            shape = RoundedCornerShape(24.dp),
            minLines = minLines,
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = (minLines * 24).dp)
                .heightIn(min = (minLines * 24).dp, max = (maxLines * 24).dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            if (showClear && onClear != null) {
                ClearAiIconButton(onClick = onClear)
            }
            SendIconButton(
                onClick = onSend,
                enabled = canSend
            )
        }
    }
}

@Composable
private fun ClearAiIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(ErrorRed.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Clear,
            contentDescription = "Clear AI text",
            tint = ErrorRed,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SendIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MessageBlue else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: SmsMessage,
    isOutgoing: Boolean,
    showTail: Boolean,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isOutgoing) MessageBlue else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isOutgoing) 18.dp else if (showTail) 4.dp else 18.dp,
        bottomEnd = if (isOutgoing) if (showTail) 4.dp else 18.dp else 18.dp
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        BoxWithConstraints {
            val bubbleMaxWidth = maxWidth * 0.94f
            Box(
                modifier = Modifier
                    .widthIn(max = bubbleMaxWidth)
                    .clip(shape)
                    .background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
            Column {
                LinkifiedMessageText(
                    text = message.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    linkColor = if (isOutgoing) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOutgoing) {
                        Color.White.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
            }
        }
    }
}

fun formatThreadListTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        isSameDay(now, then) -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        isYesterday(now, then) -> "Yesterday"
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ->
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(timestamp))
    }
}

fun formatMessageTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}

fun formatMessagePreview(body: String, direction: SmsDirection): String {
    val prefix = if (direction == SmsDirection.OUT) "You: " else ""
    return prefix + body.replace("\n", " ")
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun isYesterday(now: Calendar, then: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, then)
}

@Composable
fun MessageContextMenuDialog(
    messageText: String,
    onDismiss: () -> Unit,
    onCopy: ((String) -> Unit)? = null,
    onSuggestReply: (() -> Unit)? = null,
    onSelect: (() -> Unit)? = null,
    title: String = "Message options"
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        val copyHandler = onCopy ?: { text ->
                            copyTextToClipboard(context, text)
                        }
                        copyHandler(messageText)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Copy text")
                }
                if (onSuggestReply != null) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    TextButton(
                        onClick = {
                            onSuggestReply()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Suggest reply")
                    }
                }
                if (onSelect != null) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    TextButton(
                        onClick = {
                            onSelect()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select conversation")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MessageSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
fun MessageEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
