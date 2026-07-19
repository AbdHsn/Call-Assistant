package com.callassistant.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog

@Composable
fun KeyboardSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE) }
    var currentUri by remember {
        mutableStateOf(prefs.getString(CustomKeyboardPrefs.KEY_BACKGROUND_URI, "") ?: "")
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        currentUri = uri.toString()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Keyboard background",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (currentUri.isBlank()) "No image selected" else "Selected: $currentUri",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { launcher.launch(arrayOf("image/*")) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Choose image")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            prefs.edit().apply {
                                if (currentUri.isBlank()) {
                                    remove(CustomKeyboardPrefs.KEY_BACKGROUND_URI)
                                } else {
                                    putString(CustomKeyboardPrefs.KEY_BACKGROUND_URI, currentUri)
                                }
                                apply()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) { Text("Save") }
                }
            }
        }
    }
}

object CustomKeyboardPrefs {
    const val PREFS_NAME = "keyboard_prefs"
    const val KEY_BACKGROUND_URI = "background_uri"
}
