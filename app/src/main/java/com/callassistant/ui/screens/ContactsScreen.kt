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
import com.callassistant.ui.ContactsViewModel
import com.callassistant.ui.components.AddContactDialog
import com.callassistant.ui.components.PermissionGuard
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
    viewModel: ContactsViewModel,
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
                viewModel.syncContacts()
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
                                    importLauncher.launch(arrayOf("text/csv", "text/vcard", "text/x-vcard", "text/comma-separated-values", "text/*"))
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
                if (inSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                selectedIds = filteredContacts.map { it.id }.toSet()
                            }) { Text("Select all") }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = ErrorRed)
                            }
                            IconButton(onClick = { showBlockDialog = true }) {
                                Icon(Icons.Filled.Block, contentDescription = "Block", tint = ErrorRed)
                            }
                            IconButton(onClick = { selectedIds = emptySet() }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Cancel")
                            }
                        }
                    }
                }

                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    items(displayedContacts, key = { it.id }) { contact ->
                    val isSelected = contact.id in selectedIds
                    ContactListItem(
                        name = contact.name,
                        photoUri = contact.photoUri,
                        modifier = Modifier
                            .animateItemPlacement()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        onLongClick = {
                            selectedIds = selectedIds + contact.id
                        },
                        header = { Text(contact.name, style = MaterialTheme.typography.titleMedium) },
                        subHeader = { Text(contact.phoneNumber, style = MaterialTheme.typography.bodyMedium) },
                        trailing = {
                            if (!inSelectionMode) {
                                ActionIconButton(
                                    icon = Icons.Filled.Call,
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        if (hasPermission(Manifest.permission.CALL_PHONE)) {
                                            context.startActivity(
                                                Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.phoneNumber}"))
                                            )
                                        } else {
                                            requestPermissions()
                                        }
                                    }
                                )
                                ActionIconButton(
                                    icon = Icons.AutoMirrored.Filled.Message,
                                    color = MessageBlue,
                                    onClick = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phoneNumber}"))
                                        )
                                    }
                                )
                                Box {
                                    var expanded by remember { mutableStateOf(false) }
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                expanded = false
                                                editingContact = contact
                                                showContactDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            onClick = {
                                                expanded = false
                                                shareContact(context, contact)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("WhatsApp") },
                                            onClick = {
                                                expanded = false
                                                openWhatsApp(context, contact.phoneNumber)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("IMO") },
                                            onClick = {
                                                expanded = false
                                                openImo(context, contact.phoneNumber)
                                            }
                                        )
                                    }
                                }
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
            FloatingActionButton(
                onClick = {
                    editingContact = null
                    showContactDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add contact")
            }

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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = if (selected && inSelectionMode)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (inSelectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = onToggleSelected,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            ContactAvatar(name = name, photoUri = photoUri)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                header()
                subHeader()
            }
            trailing()
        }
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
