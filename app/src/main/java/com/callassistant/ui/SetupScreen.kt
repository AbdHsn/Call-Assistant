package com.callassistant.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.callassistant.R

@Composable
fun SetupScreen(
    hasPermission: (String) -> Boolean,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    isAccessibilityEnabled: () -> Boolean,
    onOpenAccessibility: () -> Unit,
    isBatteryOptimizationIgnored: () -> Boolean,
    onRequestBatteryOpt: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = "Call Assistant",
            modifier = Modifier.size(96.dp)
        )
        Text("Setup Call Assistant", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Grant the items below so the app can record calls.",
            style = MaterialTheme.typography.bodyMedium
        )

        val permissions = rememberPermissionsToRequest()
        val allPermissionsGranted = permissions.all { hasPermission(it) }

        SetupItem(
            title = "Dangerous permissions",
            granted = allPermissionsGranted,
            onAction = onRequestPermissions,
            actionLabel = "Grant permissions",
            secondaryLabel = "App settings",
            onSecondaryAction = onOpenAppSettings
        )

        val accessibilityEnabled = isAccessibilityEnabled()
        SetupItem(
            title = "Accessibility service",
            granted = accessibilityEnabled,
            onAction = onOpenAccessibility,
            actionLabel = "Open settings"
        )

        val batteryIgnored = isBatteryOptimizationIgnored()
        SetupItem(
            title = "Battery optimization ignored",
            granted = batteryIgnored,
            onAction = onRequestBatteryOpt,
            actionLabel = "Request exemption",
            secondaryLabel = "Battery settings",
            onSecondaryAction = onOpenBatterySettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onContinue) {
            Text("Continue")
        }
    }
}

@Composable
private fun rememberPermissionsToRequest(): List<String> {
    return remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun SetupItem(
    title: String,
    granted: Boolean,
    onAction: () -> Unit,
    actionLabel: String,
    secondaryLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (granted) "Granted" else "Required",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            if (!granted) {
                Column(horizontalAlignment = Alignment.End) {
                    OutlinedButton(onClick = onAction) {
                        Text(actionLabel)
                    }
                    if (secondaryLabel != null && onSecondaryAction != null) {
                        TextButton(onClick = onSecondaryAction) {
                            Text(secondaryLabel)
                        }
                    }
                }
            }
        }
    }
}
