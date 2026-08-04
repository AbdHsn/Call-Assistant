package com.callassistant.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.core.content.ContextCompat

@Composable
fun RecorderSettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("recorder_settings", Context.MODE_PRIVATE) }

    var autoRecord by remember { mutableStateOf(prefs.getBoolean("auto_record_enabled", true)) }
    var recordIncoming by remember { mutableStateOf(prefs.getBoolean("record_incoming", true)) }
    var recordOutgoing by remember { mutableStateOf(prefs.getBoolean("record_outgoing", true)) }
    var recordUnknown by remember { mutableStateOf(prefs.getBoolean("record_unknown", true)) }
    var audioSource by remember { mutableStateOf(prefs.getString("audio_source_override", "auto") ?: "auto") }

    val hasRecordAudio = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun save() {
        prefs.edit()
            .putBoolean("auto_record_enabled", autoRecord)
            .putBoolean("record_incoming", recordIncoming)
            .putBoolean("record_outgoing", recordOutgoing)
            .putBoolean("record_unknown", recordUnknown)
            .putString("audio_source_override", audioSource)
            .apply()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Recorder settings", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoRecord, onCheckedChange = { autoRecord = it })
                    Text("Auto-record calls")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recordIncoming, onCheckedChange = { recordIncoming = it })
                    Text("Record incoming calls")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recordOutgoing, onCheckedChange = { recordOutgoing = it })
                    Text("Record outgoing calls")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recordUnknown, onCheckedChange = { recordUnknown = it })
                    Text("Record calls with unknown numbers")
                }
            }
        }

        Text("Audio source override", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                AudioSourceOption("auto", "Auto (try best source)", audioSource) { audioSource = it }
                AudioSourceOption(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION.toString(),
                    "Voice recognition",
                    audioSource
                ) { audioSource = it }
                AudioSourceOption(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION.toString(),
                    "Voice communication",
                    audioSource
                ) { audioSource = it }
                AudioSourceOption(
                    MediaRecorder.AudioSource.VOICE_CALL.toString(),
                    "Voice call (both parties — privileged)",
                    audioSource
                ) { audioSource = it }
                AudioSourceOption(
                    MediaRecorder.AudioSource.MIC.toString(),
                    "Microphone only",
                    audioSource
                ) { audioSource = it }
            }
        }

        Button(
            onClick = { save() },
            enabled = hasRecordAudio,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save settings")
        }
    }
}

@Composable
private fun AudioSourceOption(
    value: String,
    label: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected == value,
            onClick = { onSelect(value) }
        )
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
