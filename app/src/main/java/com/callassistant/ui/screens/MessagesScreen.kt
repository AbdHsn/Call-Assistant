package com.callassistant.ui.screens

import android.telephony.SmsManager
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.data.entity.SmsDirection
import com.callassistant.permission.Permissions
import com.callassistant.ui.MainViewModel
import com.callassistant.ui.components.PermissionGuard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(
    viewModel: MainViewModel,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionGuard(
        permission = Permissions.readSms,
        hasPermission = hasPermission,
        requestPermissions = requestPermissions,
        modifier = modifier
    ) {
        val messages by viewModel.smsMessages.collectAsStateWithLifecycle()
        var showComposer by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.syncSms() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync SMS")
                        }
                    }
                }
                items(messages, key = { it.id }) { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = message.number,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${message.direction.name} · ${formatSmsDate(message.timestamp)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(text = message.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { showComposer = !showComposer },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New SMS")
            }
        }

        if (showComposer) {
            MessageComposer(
                onSend = { number, body ->
                    try {
                        val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }
                        smsManager.sendTextMessage(number, null, body, null, null)
                        viewModel.saveMessage(number, body, SmsDirection.OUT)
                    } catch (_: Exception) {
                    }
                    showComposer = false
                },
                onDismiss = { showComposer = false }
            )
        }
    }
}

@Composable
fun MessageComposer(
    onSend: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var number by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Send SMS", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("To") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    IconButton(
                        onClick = { onSend(number, body) },
                        enabled = number.isNotBlank() && body.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

private fun formatSmsDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
}
