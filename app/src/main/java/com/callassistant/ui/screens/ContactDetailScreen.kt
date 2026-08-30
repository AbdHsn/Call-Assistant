package com.callassistant.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.CallType
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.ContactSource
import com.callassistant.ui.components.AddContactDialog
import com.callassistant.ui.components.ReadOnlyLocationMap
import com.callassistant.ui.theme.ErrorRed
import com.callassistant.ui.theme.MessageBlue
import com.callassistant.ui.theme.SuccessGreen
import com.callassistant.util.ContactPhotoLoader
import com.callassistant.util.telCallUri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactDetailScreen(
    phoneNumber: String,
    contact: Contact?,
    callLogs: List<CallLogEntry>,
    onBack: () -> Unit,
    onSaveContact: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onDeleteCallLogs: (List<CallLogEntry>) -> Unit,
    onBlockNumber: (String, String?) -> Unit,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var shownContact by remember(phoneNumber) { mutableStateOf(contact) }

    LaunchedEffect(contact) {
        shownContact = when {
            contact == null -> shownContact
            shownContact == null -> contact
            contact.id == shownContact?.id -> contact.copy(
                photoUri = contact.photoUri ?: shownContact?.photoUri,
                address = contact.address ?: shownContact?.address,
                latitude = contact.latitude ?: shownContact?.latitude,
                longitude = contact.longitude ?: shownContact?.longitude
            )
            else -> contact
        }
    }

    val displayName = shownContact?.name?.takeIf { it.isNotBlank() }
        ?: callLogs.firstOrNull { it.name?.isNotBlank() == true }?.name
        ?: phoneNumber
    val sortedCallLogs = remember(callLogs) {
        callLogs.sortedByDescending { it.timestamp }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteContactDialog by remember { mutableStateOf(false) }
    var showDeleteCallLogsDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    if (showEditDialog) {
        AddContactDialog(
            contact = shownContact ?: Contact(
                name = "",
                phoneNumber = phoneNumber,
                source = ContactSource.LOCAL
            ),
            onConfirm = { saved ->
                shownContact = saved
                onSaveContact(saved)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showDeleteContactDialog && shownContact != null) {
        AlertDialog(
            onDismissRequest = { showDeleteContactDialog = false },
            title = { Text("Delete contact?") },
            text = { Text("This will permanently remove ${shownContact!!.name} from your phone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteContact(shownContact!!)
                    showDeleteContactDialog = false
                    onBack()
                }) { Text("Delete", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteContactDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteCallLogsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCallLogsDialog = false },
            title = { Text("Delete call history?") },
            text = { Text("This will permanently remove ${sortedCallLogs.size} call log entries for this number.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCallLogs(sortedCallLogs)
                    showDeleteCallLogsDialog = false
                    onBack()
                }) { Text("Delete", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCallLogsDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block $displayName?") },
            text = { Text("You will no longer receive calls or messages from this number.") },
            confirmButton = {
                TextButton(onClick = {
                    onBlockNumber(phoneNumber, displayName.takeIf { it != phoneNumber })
                    showBlockDialog = false
                    onBack()
                }) { Text("Block", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            },
            actions = {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (shownContact != null) {
                            DropdownMenuItem(
                                text = { Text("Delete contact") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = ErrorRed) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteContactDialog = true
                                }
                            )
                        }
                        if (sortedCallLogs.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Delete call history") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = ErrorRed) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteCallLogsDialog = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Block number") },
                            leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, tint = ErrorRed) },
                            onClick = {
                                menuExpanded = false
                                showBlockDialog = true
                            }
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ContactAvatar(
                            name = displayName,
                            photoUri = shownContact?.photoUri,
                            size = 96.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DetailActionChip(
                            icon = Icons.Filled.Call,
                            label = "Call",
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                if (hasPermission(Manifest.permission.CALL_PHONE)) {
                                    context.startActivity(Intent(Intent.ACTION_CALL, telCallUri(phoneNumber)))
                                } else {
                                    requestPermissions()
                                }
                            }
                        )
                        DetailActionChip(
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = "SMS",
                            color = MessageBlue,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
                                )
                            }
                        )
                        DetailActionChip(
                            label = "WA",
                            color = SuccessGreen,
                            onClick = { openWhatsApp(context, phoneNumber) }
                        )
                        DetailActionChip(
                            label = "IMO",
                            color = MaterialTheme.colorScheme.tertiary,
                            onClick = { openImo(context, phoneNumber) }
                        )
                        if (shownContact != null) {
                            DetailActionChip(
                                icon = Icons.Filled.Share,
                                label = "Share",
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = { shareContact(context, shownContact!!) }
                            )
                        }
                    }
                }
            }

            if (!shownContact?.address.isNullOrBlank()) {
                item {
                    DetailSection(title = "Address") {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = shownContact!!.address!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            val locatedContact = shownContact
            if (locatedContact?.latitude != null && locatedContact.longitude != null) {
                item {
                    DetailSection(title = "Location") {
                        Text(
                            text = "Lat ${"%.6f".format(locatedContact.latitude)}, Lng ${"%.6f".format(locatedContact.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ReadOnlyLocationMap(
                            latitude = locatedContact.latitude,
                            longitude = locatedContact.longitude,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                }
            }

            if (sortedCallLogs.isNotEmpty()) {
                item {
                    Text(
                        text = "Call history",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(sortedCallLogs, key = { it.id }) { entry ->
                    CallHistoryRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun DetailActionChip(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ActionIconButton(
            icon = icon,
            label = if (icon == null) label else null,
            color = color,
            onClick = onClick
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CallHistoryRow(entry: CallLogEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val (icon, tint) = when (entry.type) {
                CallType.INCOMING -> Icons.AutoMirrored.Filled.ArrowBack to MaterialTheme.colorScheme.primary
                CallType.OUTGOING -> Icons.AutoMirrored.Filled.ArrowForward to SuccessGreen
                CallType.MISSED -> Icons.Filled.Clear to ErrorRed
            }
            Icon(
                imageVector = icon,
                contentDescription = entry.type.name,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = formatDetailDate(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.blocked) {
                    Text(
                        text = "Blocked",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed
                    )
                }
            }
            val duration = formatDetailDuration(entry.duration)
            if (duration.isNotEmpty()) {
                Text(
                    text = duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDetailDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDetailDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}
