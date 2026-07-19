package com.callassistant.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DialPadScreen(
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    DialPadContent(
        hasPermission = hasPermission,
        requestPermissions = requestPermissions,
        modifier = modifier
    )
}

@Composable
private fun DialPadContent(
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }
    var isDefaultDialer by remember { mutableStateOf(isDefaultDialerApp(context)) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = isDefaultDialerApp(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isDefaultDialer) {
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                    }
                },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Make Call Assistant the default dialer")
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = number.ifBlank { " " },
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { number = number.dropLast(1) },
                    enabled = number.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                }
                IconButton(
                    onClick = { number = "" },
                    enabled = number.isNotEmpty()
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        }

        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { number += key },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(8.dp)
                    ) {
                        Text(text = key, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (number.isBlank()) return@Button
                    if (hasPermission(Manifest.permission.CALL_PHONE)) {
                        context.startActivity(
                            Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                        )
                    } else {
                        requestPermissions()
                    }
                },
                shape = CircleShape,
                enabled = number.isNotBlank()
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call")
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
