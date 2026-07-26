package com.callassistant.ui.screens

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.ContactSource
import com.callassistant.permission.Permissions
import com.callassistant.ui.MainViewModel
import com.callassistant.ui.components.MapLocationPickerDialog
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
        var showBlockDialog by remember { mutableStateOf(false) }
        val hasContacts = hasPermission(Permissions.contacts.permission)
        var query by remember { mutableStateOf("") }
        var sortMode by remember { mutableStateOf(ContactSortMode.NAME_ASC) }
        var selectedIds by remember { mutableStateOf(setOf<Long>()) }
        val inSelectionMode = selectedIds.isNotEmpty()
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showContactDialog by remember { mutableStateOf(false) }
        var editingContact by remember { mutableStateOf<Contact?>(null) }

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
                    }) { Text("Delete", color = Color.Red) }
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
                    }) { Text("Block", color = Color.Red) }
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
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                            IconButton(onClick = { showBlockDialog = true }) {
                                Icon(Icons.Filled.Block, contentDescription = "Block", tint = Color.Red)
                            }
                            IconButton(onClick = { selectedIds = emptySet() }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Cancel")
                            }
                        }
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredContacts, key = { it.id }) { contact ->
                    val context = LocalContext.current
                    val isSelected = contact.id in selectedIds
                    Card(
                        modifier = Modifier
                            .animateItemPlacement()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = if (isSelected)
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else CardDefaults.cardColors()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectionMode) {
                                            selectedIds = if (isSelected) selectedIds - contact.id else selectedIds + contact.id
                                        }
                                    },
                                    onLongClick = {
                                        selectedIds = selectedIds + contact.id
                                    }
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (inSelectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + contact.id else selectedIds - contact.id
                                    },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            ContactAvatar(name = contact.name, photoUri = contact.photoUri)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = contact.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!inSelectionMode) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ActionIconButton(
                                        label = "Edit",
                                        color = MaterialTheme.colorScheme.secondary,
                                        onClick = {
                                            editingContact = contact
                                            showContactDialog = true
                                        }
                                    )
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
                                        color = Color(0xFF1E88E5),
                                        onClick = {
                                            context.startActivity(
                                                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phoneNumber}"))
                                            )
                                        }
                                    )
                                    ActionIconButton(
                                        label = "WA",
                                        color = Color(0xFF25D366),
                                        onClick = { openWhatsApp(context, contact.phoneNumber) }
                                    )
                                    ActionIconButton(
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
        }
    }
}

@Composable
private fun AddContactDialog(
    contact: Contact? = null,
    onConfirm: (Contact) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = contact ?: Contact(name = "", phoneNumber = "")
    var name by remember { mutableStateOf(initial.name) }
    var phone by remember { mutableStateOf(initial.phoneNumber) }
    var photoUri by remember { mutableStateOf(initial.photoUri ?: "") }
    var address by remember { mutableStateOf(initial.address ?: "") }
    var latitude by remember { mutableStateOf(initial.latitude) }
    var longitude by remember { mutableStateOf(initial.longitude) }
    var showMapPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.toString()?.let { photoUri = it }
    }

    LaunchedEffect(photoUri) {
        preview = if (photoUri.isBlank()) {
            null
        } else {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
            bitmap
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (contact != null) "Edit contact" else "New contact") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (preview != null) {
                        Image(
                            bitmap = preview!!,
                            contentDescription = "Selected photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Add photo", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Text("Choose photo")
                    }
                    if (photoUri.isNotBlank()) {
                        TextButton(onClick = { photoUri = "" }) {
                            Text("Remove")
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Location", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (latitude != null && longitude != null) {
                                "Lat: %.6f  Lng: %.6f".format(latitude, longitude)
                            } else {
                                "No location selected"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { showMapPicker = true }) {
                        Text(if (latitude != null) "Change" else "Pick on map")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        val updated = contact?.copy(
                            name = name.trim(),
                            phoneNumber = phone.trim(),
                            photoUri = photoUri.trim().takeIf { it.isNotBlank() },
                            address = address.trim().takeIf { it.isNotBlank() },
                            latitude = latitude,
                            longitude = longitude
                        ) ?: Contact(
                            name = name.trim(),
                            phoneNumber = phone.trim(),
                            source = ContactSource.LOCAL,
                            photoUri = photoUri.trim().takeIf { it.isNotBlank() },
                            address = address.trim().takeIf { it.isNotBlank() },
                            latitude = latitude,
                            longitude = longitude
                        )
                        onConfirm(updated)
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showMapPicker) {
        MapLocationPickerDialog(
            initialLatitude = latitude,
            initialLongitude = longitude,
            onConfirm = { lat, lng ->
                latitude = lat
                longitude = lng
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    }
}

private val avatarColors = listOf(
    Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF3949AB), Color(0xFF1E88E5),
    Color(0xFF00ACC1), Color(0xFF43A047), Color(0xFFF4511E), Color(0xFFD81B60),
    Color(0xFF00897B), Color(0xFF6D4C41), Color(0xFF039BE5), Color(0xFF7CB342)
)

@Composable
internal fun ContactAvatar(name: String, photoUri: String? = null) {
    val context = LocalContext.current
    var bitmap by remember(photoUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photoUri) {
        bitmap = if (photoUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    val color = avatarColors[name.hashCode().and(0x7FFFFFFF) % avatarColors.size]
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val background = if (bitmap == null) color else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .size(40.dp)
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
            Text(text = initial, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
