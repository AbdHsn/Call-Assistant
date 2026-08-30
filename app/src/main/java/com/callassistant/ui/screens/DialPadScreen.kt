package com.callassistant.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.os.Build
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.R
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.Contact
import com.callassistant.ui.DialPadViewModel
import com.callassistant.ui.components.AddContactDialog
import com.callassistant.ui.theme.MessageBlue
import com.callassistant.util.telCallUri
import kotlinx.coroutines.delay

@Composable
fun DialPadScreen(
    viewModel: DialPadViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    DialPadContent(
        viewModel = viewModel,
        hasPermission = hasPermission,
        requestPermissions = requestPermissions,
        modifier = modifier
    )
}

private val dialKeys = listOf(
    Triple("1", "", ""),
    Triple("2", "ABC", ""),
    Triple("3", "DEF", ""),
    Triple("4", "GHI", ""),
    Triple("5", "JKL", ""),
    Triple("6", "MNO", ""),
    Triple("7", "PQRS", ""),
    Triple("8", "TUV", ""),
    Triple("9", "WXYZ", ""),
    Triple("*", "", ""),
    Triple("0", "+", ""),
    Triple("#", "", "")
)

private fun Char.toT9Digit(): Char? = when (uppercaseChar()) {
    in 'A'..'C' -> '2'
    in 'D'..'F' -> '3'
    in 'G'..'I' -> '4'
    in 'J'..'L' -> '5'
    in 'M'..'O' -> '6'
    in 'P'..'S' -> '7'
    in 'T'..'V' -> '8'
    in 'W'..'Z' -> '9'
    in '0'..'9' -> this
    else -> null
}

private data class T9Contact(
    val contact: com.callassistant.data.entity.Contact,
    val normalizedNumber: String,
    val wordT9s: List<Pair<String, IntRange>>,
    val initialsT9: String,
    val initialIndices: List<Int>,
    val callCount: Int,
    val lastCall: Long
)

private data class ContactMatch(
    val contact: com.callassistant.data.entity.Contact,
    val name: AnnotatedString,
    val number: AnnotatedString,
    val callCount: Int,
    val lastCall: Long
)

private fun com.callassistant.data.entity.Contact.toT9Contact(score: Pair<Int, Long>): T9Contact {
    val normalized = phoneNumber.filter { it.isDigit() }
    val wordT9s = mutableListOf<Pair<String, IntRange>>()
    val initialsT9 = StringBuilder()
    val initialIndices = mutableListOf<Int>()
    Regex("[A-Za-z0-9]+").findAll(name).forEach { match ->
        val range = match.range
        val t9 = match.value.mapNotNull { it.toT9Digit() }.joinToString("")
        if (t9.isNotEmpty()) {
            wordT9s.add(t9 to range)
            initialsT9.append(t9[0])
            initialIndices.add(range.first)
        }
    }
    return T9Contact(
        contact = this,
        normalizedNumber = normalized,
        wordT9s = wordT9s,
        initialsT9 = initialsT9.toString(),
        initialIndices = initialIndices,
        callCount = score.first,
        lastCall = score.second
    )
}

private fun T9Contact.match(digits: String): ContactMatch? {
    val nameHighlights = mutableListOf<IntRange>()
    val numberHighlights = mutableListOf<IntRange>()
    var matched = false

    val phoneNumber = contact.phoneNumber
    if (normalizedNumber.contains(digits)) {
        matched = true
        val digitToOriginal = mutableListOf<Int>()
        phoneNumber.forEachIndexed { index, c ->
            if (c.isDigit()) digitToOriginal.add(index)
        }
        var start = 0
        while (true) {
            val idx = normalizedNumber.indexOf(digits, start)
            if (idx < 0) break
            val originalStart = digitToOriginal[idx]
            val originalEnd = digitToOriginal[idx + digits.length - 1] + 1
            numberHighlights.add(originalStart until originalEnd)
            start = idx + 1
        }
    }

    if (initialsT9.startsWith(digits)) {
        matched = true
        for (i in digits.indices) {
            val index = initialIndices[i]
            nameHighlights.add(index..index)
        }
    } else {
        for ((t9, range) in wordT9s) {
            if (t9.startsWith(digits)) {
                matched = true
                for (i in digits.indices) {
                    nameHighlights.add(range.first + i..range.first + i)
                }
                break
            }
        }
    }

    if (!matched) return null

    val annotatedName = buildAnnotatedString {
        append(contact.name)
        nameHighlights.forEach {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), it.first, it.last + 1)
        }
    }
    val annotatedNumber = buildAnnotatedString {
        append(phoneNumber)
        numberHighlights.forEach {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), it.first, it.last + 1)
        }
    }

    return ContactMatch(contact, annotatedName, annotatedNumber, callCount, lastCall)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialPadContent(
    viewModel: DialPadViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf(TextFieldValue("")) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }
    var isDefaultDialer by remember { mutableStateOf(isDefaultDialerApp(context)) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contacts = uiState.contacts
    val callLogs = uiState.callLogs

    val scoreMap = remember(callLogs) {
        callLogs.groupBy { it.number.filter { c -> c.isDigit() } }
            .mapValues { (_, logs) ->
                logs.size to (logs.maxOfOrNull { it.timestamp } ?: 0L)
            }
    }

    val t9Contacts = remember(contacts, scoreMap) {
        contacts.map { contact ->
            val normalized = contact.phoneNumber.filter { it.isDigit() }
            contact.toT9Contact(scoreMap[normalized] ?: (0 to 0L))
        }
    }

    var debouncedInput by remember { mutableStateOf("") }
    LaunchedEffect(number.text) {
        if (number.text.isEmpty()) {
            debouncedInput = ""
        } else {
            delay(150)
            debouncedInput = number.text
        }
    }

    val searchDigits = debouncedInput.filter { it.isDigit() }
    val suggestions = remember(t9Contacts, searchDigits) {
        if (searchDigits.isEmpty()) emptyList()
        else t9Contacts.mapNotNull { it.match(searchDigits) }
            .sortedWith(
                compareByDescending<ContactMatch> { it.callCount }
                    .thenByDescending { it.lastCall }
                    .thenBy { it.contact.name.lowercase() }
            )
            .take(20)
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = isDefaultDialerApp(context)
    }

    fun placeCall(target: String = number.text) {
        if (target.isBlank()) return
        if (hasPermission(Manifest.permission.CALL_PHONE)) {
            context.startActivity(Intent(Intent.ACTION_CALL, telCallUri(target)))
        } else {
            requestPermissions()
        }
    }

    fun insertDigit(digit: String) {
        val text = number.text
        val selection = number.selection
        val newText = text.substring(0, selection.start) + digit + text.substring(selection.end)
        val newCursor = selection.start + digit.length
        number = TextFieldValue(newText, TextRange(newCursor))
    }

    fun deleteDigit() {
        val text = number.text
        val selection = number.selection
        if (!selection.collapsed) {
            val newText = text.removeRange(selection.start, selection.end)
            number = TextFieldValue(newText, TextRange(selection.start))
        } else if (selection.start > 0) {
            val newText = text.removeRange(selection.start - 1, selection.start)
            number = TextFieldValue(newText, TextRange(selection.start - 1))
        }
    }

    fun normalizeBangladeshNumber(number: String): String {
        val cleaned = number.filter { it.isDigit() }
        return when {
            number.startsWith("+880") -> number
            cleaned.startsWith("880") -> "+$cleaned"
            cleaned.startsWith("0") && cleaned.length == 11 -> "+880${cleaned.drop(1)}"
            cleaned.startsWith("1") && cleaned.length == 10 -> "+880$cleaned"
            else -> number
        }
    }

    fun findWhatsAppCallDataId(normalizedDigits: String): Long? {
        val key = normalizedDigits.takeLast(10)
        return try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID, ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf("vnd.android.cursor.item/vnd.com.whatsapp.voip.call"),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val raw = cursor.getString(numberIdx) ?: continue
                    val rawDigits = raw.filter { it.isDigit() }
                    if (rawDigits.takeLast(10) == key) {
                        return cursor.getLong(idIdx)
                    }
                }
                null
            }
        } catch (_: SecurityException) {
            null
        }
    }

    fun placeWhatsAppCall(target: String = number.text) {
        if (target.isBlank()) return
        val normalized = normalizeBangladeshNumber(target)
        val digits = normalized.filter { it.isDigit() }
        val dataId = findWhatsAppCallDataId(digits)
        if (dataId != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
                )
                setPackage("com.whatsapp")
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            }
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    if (editingContact != null) {
        AddContactDialog(
            contact = editingContact,
            onConfirm = { contact ->
                viewModel.saveContact(contact)
                editingContact = null
            },
            onDismiss = { editingContact = null }
        )
    }

    if (showAddDialog) {
        AddContactDialog(
            contact = Contact(name = "", phoneNumber = number.text),
            onConfirm = { contact ->
                viewModel.saveContact(contact)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = !isDefaultDialer) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Set Call Assistant as your default dialer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                                roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                            }
                        }
                    ) {
                        Text("Set", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            Text(
                text = "Suggested contacts",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions, key = { it.contact.id }) { match ->
                    ContactListItem(
                        name = match.contact.name,
                        photoUri = match.contact.photoUri,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { number = TextFieldValue(match.contact.phoneNumber, TextRange(match.contact.phoneNumber.length)) },
                        header = { Text(match.name, style = MaterialTheme.typography.titleMedium) },
                        subHeader = { Text(match.number, style = MaterialTheme.typography.bodyMedium) },
                        trailing = {
                            ActionIconButton(
                                icon = Icons.Filled.Call,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { placeCall(match.contact.phoneNumber) }
                            )
                            ActionIconButton(
                                icon = Icons.AutoMirrored.Filled.Message,
                                color = MessageBlue,
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${match.contact.phoneNumber}"))
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
                                            editingContact = match.contact
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        onClick = {
                                            expanded = false
                                            shareContact(context, match.contact)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("WhatsApp") },
                                        onClick = {
                                            expanded = false
                                            openWhatsApp(context, match.contact.phoneNumber)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("IMO") },
                                        onClick = {
                                            expanded = false
                                            openImo(context, match.contact.phoneNumber)
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        } else if (number.text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No results",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { if (number.text.isNotBlank()) showAddDialog = true },
                enabled = number.text.isNotBlank(),
                shape = CircleShape,
                color = if (number.text.isNotBlank()) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add contact",
                        tint = if (number.text.isNotBlank()) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = number,
                onValueChange = { newValue ->
                    val allowed = newValue.text.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                    number = if (allowed == newValue.text) newValue else TextFieldValue(allowed, TextRange(allowed.length))
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (number.text.isEmpty()) {
                            Text(
                                text = "Enter a number",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            dialKeys.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { (digit, letters, _) ->
                        DialKey(
                            digit = digit,
                            letters = letters,
                            onClick = { insertDigit(digit) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Surface(
                onClick = { placeWhatsAppCall() },
                enabled = number.text.isNotBlank(),
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp call",
                        tint = if (number.text.isNotBlank()) Color.Unspecified
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Surface(
                onClick = { placeCall() },
                enabled = number.text.isNotBlank(),
                shape = RoundedCornerShape(28.dp),
                color = if (number.text.isNotBlank()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .width(120.dp)
                    .height(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = "Call",
                        tint = if (number.text.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(56.dp)
                    .combinedClickable(
                        enabled = number.text.isNotEmpty(),
                        onClick = { deleteDigit() },
                        onLongClick = { number = TextFieldValue("") }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete (hold to clear)",
                        tint = if (number.text.isNotEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialKey(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "dialKeyScale"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        interactionSource = interactionSource,
        modifier = modifier
            .height(64.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = digit,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (letters.isNotEmpty()) {
                    Text(
                        text = letters,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}


private fun isDefaultDialerApp(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        telecomManager.defaultDialerPackage == context.packageName
    }
}
