package com.callassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.ai.model.AiContextScope
import com.callassistant.ai.model.AiModelCatalog
import com.callassistant.ai.model.AiModelDefinition
import com.callassistant.ai.model.AiModelStatus
import com.callassistant.ai.model.AiModelVariant
import com.callassistant.ui.messageai.MessageAiViewModel

@Composable
fun AiSettingsScreen(
    viewModel: MessageAiViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val modelStatus = uiState.modelStatus
    val selectedDefinition = AiModelCatalog.get(uiState.selectedModel)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI Message Assistant",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (uiState.selectedModel) {
                        AiModelVariant.AZURE_OPENAI -> "Cloud message suggestions via Azure OpenAI"
                        else -> "Offline on-device message suggestions"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = buildString {
                append(
                    if (uiState.nativeRuntimeAvailable) {
                        "Native runtime: available (arm64)"
                    } else {
                        "Native runtime: unavailable — using smart templates"
                    }
                )
                append(" · Device RAM: ")
                append(String.format("%.1f GB", uiState.deviceRamGb))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AI model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pick a model for your phone, or use Azure OpenAI when online.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                AiModelCatalog.all.forEach { definition ->
                    ModelOptionRow(
                        definition = definition,
                        selected = uiState.selectedModel == definition.variant,
                        supported = viewModel.isModelSupported(definition.variant),
                        downloaded = viewModel.isModelDownloaded(definition.variant),
                        onSelect = { viewModel.selectModel(definition.variant) }
                    )
                }
            }
        }

        if (uiState.selectedModel.isOnDevice) {
            OnDeviceModelStatusCard(
                selectedDefinition = selectedDefinition,
                modelStatus = modelStatus,
                selectedModel = uiState.selectedModel,
                isModelSupported = { viewModel.isModelSupported(it) },
                onDelete = { viewModel.deleteModel() },
                onCancelDownload = { viewModel.cancelDownload() },
                onDownload = { viewModel.downloadModel() }
            )
        } else {
            AzureModelStatusCard(
                configured = uiState.azureConfigured,
                modelName = uiState.azureModelName
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Default context",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "How many messages AI reads when suggesting replies",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                AiContextScope.entries.forEach { scope ->
                    OutlinedButton(
                        onClick = { viewModel.setContextScope(scope) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = scope.label + if (scope == AiContextScope.DEFAULT) " (recommended)" else ""
                        )
                    }
                }
            }
        }

        Text(
            text = "Suggestions are inserted into your message — never sent automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun ModelOptionRow(
    definition: AiModelDefinition,
    selected: Boolean,
    supported: Boolean,
    downloaded: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onSelect,
        enabled = supported || selected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect, enabled = supported || selected)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = definition.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = definition.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!supported && definition.variant != AiModelVariant.AZURE_OPENAI) {
                    Text(
                        text = "Not enough RAM on this device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (definition.variant == AiModelVariant.AZURE_OPENAI && !downloaded) {
                    Text(
                        text = "Add credentials to local.properties and rebuild",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (downloaded) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format("%.1f GB", mb / 1024.0) else String.format("%.0f MB", mb)
}

@Composable
private fun OnDeviceModelStatusCard(
    selectedDefinition: AiModelDefinition,
    modelStatus: AiModelStatus,
    selectedModel: AiModelVariant,
    isModelSupported: (AiModelVariant) -> Boolean,
    onDelete: () -> Unit,
    onCancelDownload: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Model status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = selectedDefinition.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (modelStatus) {
                is AiModelStatus.Ready -> {
                    Text(
                        text = "Ready — downloaded",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatSize(modelStatus.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete ${selectedDefinition.displayName}")
                    }
                }
                is AiModelStatus.Downloading -> {
                    Text(
                        text = "Downloading ${selectedDefinition.displayName}… ${(modelStatus.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { modelStatus.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${formatSize(modelStatus.bytesDone)} / ${formatSize(modelStatus.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onCancelDownload) {
                        Text("Cancel")
                    }
                }
                is AiModelStatus.UnsupportedDevice -> {
                    Text(
                        text = "Selected model needs more RAM for this device",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${selectedDefinition.displayName} requires ${selectedDefinition.minRamGb.toInt()} GB+ RAM (arm64)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try Qwen3.5-0.8B for lower-RAM phones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is AiModelStatus.Error -> {
                    Text(
                        text = modelStatus.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onDownload) {
                        Text("Retry download")
                    }
                }
                is AiModelStatus.NotDownloaded -> {
                    Text(
                        text = "Not downloaded",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Approx. ${formatSize(selectedDefinition.sizeBytes)} · Wi‑Fi only by default",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDownload,
                        enabled = isModelSupported(selectedModel)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download ${selectedDefinition.displayName}")
                    }
                }
            }
        }
    }
}

@Composable
private fun AzureModelStatusCard(
    configured: Boolean,
    modelName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cloud status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Azure OpenAI",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (configured) {
                Text(
                    text = "Ready — $modelName",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Uses your internet connection. Credentials are read from local.properties at build time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "Not configured",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Add azure.openai.* values to local.properties, then rebuild the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
