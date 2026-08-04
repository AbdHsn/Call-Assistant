package com.callassistant.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.ContactSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AddContactDialog(
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
    var cropSource by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.toString()?.let { cropSource = it }
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
                        .clickable {
                            if (photoUri.isBlank()) galleryLauncher.launch("image/*")
                            else cropSource = photoUri
                        },
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
                        TextButton(onClick = { cropSource = photoUri }) {
                            Text("Edit")
                        }
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

    cropSource?.let { src ->
        PhotoCropDialog(
            sourceUri = src,
            onConfirm = { cropped ->
                photoUri = cropped
                cropSource = null
            },
            onDismiss = { cropSource = null }
        )
    }
}
