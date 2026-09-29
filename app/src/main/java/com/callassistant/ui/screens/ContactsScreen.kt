package com.callassistant.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.ContactSource
import com.callassistant.permission.Permissions
import com.callassistant.ui.phonebook.PhoneBookViewModel
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import com.callassistant.ui.components.AddContactDialog
import com.callassistant.ui.components.MessageEmptyState
import com.callassistant.ui.components.PhoneBookListSkeleton
import com.callassistant.ui.components.PermissionGuard
import com.callassistant.ui.components.PhoneBookListDivider
import com.callassistant.ui.components.PhoneBookContactActions
import com.callassistant.ui.components.PhoneBookOverflowAction
import com.callassistant.ui.util.ResponsiveScreenContainer
import com.callassistant.ui.util.rememberListLayoutMetrics
import com.callassistant.ui.components.PhoneBookSearchRow
import com.callassistant.ui.components.PhoneBookSelectionBar
import com.callassistant.ui.components.PhoneBookTonalActionButton
import com.callassistant.ui.theme.ErrorRed
import com.callassistant.ui.theme.MessageBlue
import com.callassistant.ui.util.rememberScrollPagination
import com.callassistant.util.PhoneNumberNormalizer
import com.callassistant.util.ContactPhotoLoader

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
    viewModel: PhoneBookViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    onOpenMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionGuard(
        permission = Permissions.contacts,
        hasPermission = hasPermission,
        requestPermissions = requestPermissions,
        modifier = modifier
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val contacts = uiState.contacts
        val callLogs = uiState.callLogs
        var showBlockDialog by remember { mutableStateOf(false) }
        val hasContacts = hasPermission(Permissions.contacts.permission)
        var query by remember { mutableStateOf("") }
        var sortMode by remember { mutableStateOf(ContactSortMode.NAME_ASC) }
        var selectedIds by remember { mutableStateOf(setOf<Long>()) }
        val inSelectionMode = selectedIds.isNotEmpty()
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showContactDialog by remember { mutableStateOf(false) }
        var editingContact by remember { mutableStateOf<Contact?>(null) }
        var selectedContact by remember { mutableStateOf<Contact?>(null) }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""
                    val parsed = if (text.contains("BEGIN:VCARD", ignoreCase = true)) parseVCard(text) else parseCsv(text)
                    viewModel.importContacts(parsed)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Imported ${parsed.size} contacts", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                try {
                    val csv = writeCsv(viewModel.uiState.value.contacts)
                    context.contentResolver.openOutputStream(uri)?.use { out -> out.write(csv.toByteArray()) }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Exported CSV", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val exportVCardLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/vcard")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                try {
                    val vCard = writeVCard(viewModel.uiState.value.contacts)
                    context.contentResolver.openOutputStream(uri)?.use { out -> out.write(vCard.toByteArray()) }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Exported vCard", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

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
                viewModel.ensureContactsSynced()
            }
        }

        val listState = rememberLazyListState()
        val displayLimit = rememberScrollPagination(
            totalItemCount = filteredContacts.size,
            listState = listState,
            resetKey = query to sortMode
        )
        val displayedContacts = filteredContacts.take(displayLimit)
        val hasMoreContacts = displayLimit < filteredContacts.size

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete ${selectedIds.size} contact${if (selectedIds.size > 1) "s" else ""}?") },
                text = { Text("This will permanently remove them from your phone and sync with Google.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteContacts(filteredContacts.filter { it.id in selectedIds })
                        selectedIds = emptySet()
                        showDeleteDialog = false
                    }) { Text("Delete", color = ErrorRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showBlockDialog) {
            val contactsToBlock = filteredContacts.filter { it.id in selectedIds }
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title = { Text("Block ${contactsToBlock.size} contact${if (contactsToBlock.size > 1) "s" else ""}?") },
                text = { Text("You will no longer receive calls or messages from these numbers.") },
                confirmButton = {
                    TextButton(onClick = {
                        contactsToBlock.forEach { viewModel.blockNumber(it.phoneNumber, name = it.name) }
                        selectedIds = emptySet()
                        showBlockDialog = false
                    }) { Text("Block", color = ErrorRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showContactDialog) {
            AddContactDialog(
                contact = editingContact,
                onConfirm = { contact ->
                    viewModel.saveContact(contact)
                    showContactDialog = false
                    editingContact = null
                },
                onDismiss = {
                    showContactDialog = false
                    editingContact = null
                }
            )
        }

        val listMetrics = rememberListLayoutMetrics(inSelectionMode)

        Box(modifier = Modifier.fillMaxSize()) {
            ResponsiveScreenContainer {
            Column(modifier = Modifier.fillMaxSize()) {
                PhoneBookSearchRow(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search contacts",
                    sortOptions = ContactSortMode.entries.map { it to it.label },
                    onSortSelected = { selected ->
                        sortMode = selected as ContactSortMode
                    },
                    trailingActions = {
                        Box {
                            var exportMenuExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { exportMenuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Import / Export")
                            }
                            DropdownMenu(
                                expanded = exportMenuExpanded,
                                onDismissRequest = { exportMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Import") },
                                    onClick = {
                                        exportMenuExpanded = false
                                        importLauncher.launch(
                                            arrayOf(
                                                "text/csv",
                                                "text/vcard",
                                                "text/x-vcard",
                                                "text/comma-separated-values",
                                                "text/*"
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export CSV") },
                                    onClick = {
                                        exportMenuExpanded = false
                                        exportCsvLauncher.launch("contacts.csv")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export vCard") },
                                    onClick = {
                                        exportMenuExpanded = false
                                        exportVCardLauncher.launch("contacts.vcf")
                                    }
                                )
                            }
                        }
                    }
                )

                if (inSelectionMode) {
                    PhoneBookSelectionBar(
                        selectedCount = selectedIds.size,
                        onSelectAll = { selectedIds = filteredContacts.map { it.id }.toSet() },
                        onDelete = { showDeleteDialog = true },
                        onBlock = { showBlockDialog = true },
                        onCancel = { selectedIds = emptySet() }
                    )
                }

                if (uiState.showContactsSkeleton) {
                    PhoneBookListSkeleton(
                        modifier = Modifier.weight(1f)
                    )
                } else if (filteredContacts.isEmpty()) {
                    MessageEmptyState(
                        title = if (query.isBlank()) "No contacts" else "No results",
                        subtitle = if (query.isBlank()) {
                            "Tap Add contact to create one or import from file"
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
                        contentPadding = PaddingValues(bottom = listMetrics.fabClearance)
                    ) {
                        items(displayedContacts, key = { it.id }) { contact ->
                            val isSelected = contact.id in selectedIds
                            ContactListItem(
                                name = contact.name,
                                photoUri = contact.photoUri,
                                modifier = Modifier.animateItemPlacement(),
                                selected = isSelected,
                                inSelectionMode = inSelectionMode,
                                onToggleSelected = { checked ->
                                    selectedIds = if (checked) selectedIds + contact.id else selectedIds - contact.id
                                },
                                onClick = {
                                    if (inSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - contact.id else selectedIds + contact.id
                                    } else {
                                        selectedContact = contact
                                    }
                                },
                                onLongClick = { selectedIds = selectedIds + contact.id },
                                header = {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                subHeader = {
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailing = {
                                    if (!inSelectionMode) {
                                        PhoneBookContactActions(
                                            onCall = {
                                                when (com.callassistant.util.CallPlacer.placeCallWithFeedback(
                                                    context,
                                                    contact.phoneNumber
                                                )) {
                                                    com.callassistant.util.CallPlaceResult.NeedPermission ->
                                                        requestPermissions()
                                                    else -> Unit
                                                }
                                            },
                                            onMessage = { onOpenMessage(contact.phoneNumber) },
                                            overflowActions = listOf(
                                                PhoneBookOverflowAction("Edit") {
                                                    editingContact = contact
                                                    showContactDialog = true
                                                },
                                                PhoneBookOverflowAction("Share") {
                                                    shareContact(context, contact)
                                                },
                                                PhoneBookOverflowAction("WhatsApp") {
                                                    openWhatsApp(context, contact.phoneNumber)
                                                },
                                                PhoneBookOverflowAction("IMO") {
                                                    openImo(context, contact.phoneNumber)
                                                }
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        if (hasMoreContacts) {
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
            ExtendedFloatingActionButton(
                onClick = {
                    editingContact = null
                    showContactDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add contact") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )

            selectedContact?.let { selected ->
                val liveContact = contacts.find { it.id == selected.id }?.let { fromDb ->
                    fromDb.copy(
                        photoUri = selected.photoUri ?: fromDb.photoUri,
                        name = selected.name.ifBlank { fromDb.name },
                        address = selected.address ?: fromDb.address,
                        latitude = selected.latitude ?: fromDb.latitude,
                        longitude = selected.longitude ?: fromDb.longitude
                    )
                } ?: selected
                val relatedCallLogs = callLogs.filter {
                    PhoneNumberNormalizer.matches(it.number, liveContact.phoneNumber)
                }
                ContactDetailScreen(
                    phoneNumber = liveContact.phoneNumber,
                    contact = liveContact,
                    callLogs = relatedCallLogs,
                    onBack = { selectedContact = null },
                    onSaveContact = { saved ->
                        viewModel.saveContact(saved)
                        selectedContact = saved
                    },
                    onDeleteContact = { contact -> viewModel.deleteContacts(listOf(contact)) },
                    onDeleteCallLogs = viewModel::deleteCallLogs,
                    onBlockNumber = { number, name -> viewModel.blockNumber(number, name = name) },
                    onOpenMessage = { number ->
                        selectedContact = null
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

private val avatarColors = listOf(
    Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF3949AB), Color(0xFF1E88E5),
    Color(0xFF00ACC1), Color(0xFF43A047), Color(0xFFF4511E), Color(0xFFD81B60),
    Color(0xFF00897B), Color(0xFF6D4C41), Color(0xFF039BE5), Color(0xFF7CB342)
)

@Composable
internal fun ContactAvatar(
    name: String,
    photoUri: String? = null,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    val context = LocalContext.current
    var bitmap by remember(photoUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photoUri) {
        bitmap = if (photoUri.isNullOrBlank()) {
            null
        } else {
            ContactPhotoLoader.loadBitmap(context, photoUri)
        }
    }

    val color = avatarColors[name.hashCode().and(0x7FFFFFFF) % avatarColors.size]
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val background = if (bitmap == null) color else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Contact photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initial,
                color = Color.White,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun ActionIconButton(
    color: Color,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String? = null
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        } else if (label != null) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContactListItem(
    name: String,
    photoUri: String?,
    header: @Composable () -> Unit,
    subHeader: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
    onToggleSelected: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val metrics = rememberListLayoutMetrics(inSelectionMode)
    val background = if (selected && inSelectionMode) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = modifier
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
                    checked = selected,
                    onCheckedChange = onToggleSelected
                )
            }
            ContactAvatar(name = name, photoUri = photoUri, size = metrics.avatarSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                header()
                subHeader()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                content = trailing
            )
        }
        PhoneBookListDivider(leadingInset = metrics.dividerInset)
    }
}

internal fun openWhatsApp(context: android.content.Context, phoneNumber: String) {
    val digits = phoneNumber.filter { it.isDigit() }
    val uri = Uri.parse("https://wa.me/$digits")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.whatsapp")
    }
    startOrFallback(context, intent, "WhatsApp not installed")
}

internal fun openImo(context: android.content.Context, phoneNumber: String) {
    val digits = phoneNumber.filter { it.isDigit() }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("imo://user/$digits"))
    startOrFallback(context, intent, "IMO not installed")
}

internal fun startOrFallback(context: android.content.Context, intent: Intent, fallbackMessage: String) {
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

internal fun shareContact(context: android.content.Context, contact: com.callassistant.data.entity.Contact) {
    val shareText = "Contact: ${contact.name}\nPhone: ${contact.phoneNumber}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, contact.name)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share contact"))
}

internal fun parseCsv(text: String): List<Contact> {
    val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()
    val header = parseCsvLine(lines.first()).map { it.trim().lowercase().replace("\"", "") }
    val nameIdx = header.indexOf("name").takeIf { it >= 0 } ?: 0
    val phoneIdx = header.indexOf("phone").takeIf { it >= 0 } ?: 1
    return lines.drop(1).mapNotNull { line ->
        val cols = parseCsvLine(line)
        val name = cols.getOrNull(nameIdx)?.trim()?.removeSurrounding("\"") ?: return@mapNotNull null
        val phone = cols.getOrNull(phoneIdx)?.trim()?.removeSurrounding("\"") ?: return@mapNotNull null
        if (name.isBlank() || phone.isBlank()) return@mapNotNull null
        Contact(name = name, phoneNumber = phone, source = ContactSource.IMPORTED)
    }
}

internal fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    for (c in line) {
        when {
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                result.add(current.toString())
                current.clear()
            }
            else -> current.append(c)
        }
    }
    result.add(current.toString())
    return result
}

internal fun parseVCard(text: String): List<Contact> {
    val cards = text.split("BEGIN:VCARD").drop(1)
    return cards.mapNotNull { card ->
        val lines = card.lines().map { it.trim() }
        if (!card.contains("END:VCARD")) return@mapNotNull null
        val name = lines.firstOrNull { it.startsWith("FN:") }?.substringAfter("FN:")?.trim()
            ?: lines.firstOrNull { it.startsWith("N:") }?.substringAfter("N:")?.trim()?.split(";")?.let { parts ->
                val first = parts.getOrNull(1)?.trim() ?: ""
                val last = parts.getOrNull(0)?.trim() ?: ""
                "$first $last".trim().ifBlank { null }
            }
        val phone = lines.firstOrNull { it.startsWith("TEL") }?.substringAfterLast(":")?.trim()
            ?.takeIf { it.isNotBlank() }
        if (name == null || phone == null) return@mapNotNull null
        Contact(name = name, phoneNumber = phone, source = ContactSource.IMPORTED)
    }
}

internal fun writeCsv(contacts: List<Contact>): String {
    val header = "name,phone"
    val rows = contacts.joinToString("\n") { contact ->
        "\"${contact.name}\",\"${contact.phoneNumber}\""
    }
    return if (contacts.isEmpty()) header else "$header\n$rows"
}

internal fun writeVCard(contacts: List<Contact>): String {
    return contacts.joinToString("\n") { contact ->
        "BEGIN:VCARD\nVERSION:2.1\nFN:${contact.name}\nTEL:${contact.phoneNumber}\nEND:VCARD"
    }
}
