package com.callassistant.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callassistant.ui.screens.CallLogScreen
import com.callassistant.ui.screens.ContactsScreen
import com.callassistant.ui.screens.DialPadScreen
import com.callassistant.ui.screens.MessagesScreen
import com.callassistant.ui.screens.NotesScreen
import com.callassistant.ui.screens.RecorderSettingsScreen
import com.callassistant.ui.screens.RecordingsScreen
import com.callassistant.ui.SetupScreen
import com.callassistant.ui.screens.SpamRulesScreen
import com.callassistant.ui.theme.ThemeMode

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Contacts : Screen("contacts", "Contacts", Icons.Default.Person)
    object CallLog : Screen("call_log", "Call Log", Icons.Default.Call)
    object DialPad : Screen("dial_pad", "Dial", Icons.Default.Dialpad)
    object SpamRules : Screen("spam_rules", "Spam", Icons.Default.Shield)
    object Messages : Screen("messages", "Msg", Icons.AutoMirrored.Filled.Message)
    object Recordings : Screen("recordings", "Records", Icons.Default.Mic)
    object RecorderSettings : Screen("recorder_settings", "Recorder", Icons.Default.Settings)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    factory: MainViewModelFactory,
    requestPermissions: () -> Unit,
    hasPermission: (String) -> Boolean,
    isAccessibilityEnabled: () -> Boolean,
    isBatteryIgnored: () -> Boolean,
    requestBatteryOpt: () -> Unit,
    openBatterySettings: () -> Unit,
    openAppSettings: () -> Unit,
    openAccessibility: () -> Unit,
    viewModel: MainViewModel = viewModel(factory = factory)
) {
    val screens = listOf(Screen.CallLog, Screen.Contacts, Screen.DialPad, Screen.Messages)
    val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Assistant", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Notes") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.selectRoute("notes")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Theme") },
                                onClick = {
                                    menuExpanded = false
                                    showThemeDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Spam & Block Numbers") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.selectRoute(Screen.SpamRules.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Records") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.selectRoute(Screen.Recordings.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Recorder settings") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.selectRoute(Screen.RecorderSettings.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.selectRoute(Screen.Settings.route)
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedRoute == screen.route,
                        onClick = { viewModel.selectRoute(screen.route) },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (selectedRoute) {
            Screen.Contacts.route -> ContactsScreen(
                viewModel = viewModel,
                hasPermission = hasPermission,
                requestPermissions = requestPermissions,
                modifier = modifier
            )
            Screen.CallLog.route -> CallLogScreen(
                viewModel = viewModel,
                hasPermission = hasPermission,
                requestPermissions = requestPermissions,
                modifier = modifier
            )
            Screen.DialPad.route -> DialPadScreen(
                viewModel = viewModel,
                hasPermission = hasPermission,
                requestPermissions = requestPermissions,
                modifier = modifier
            )
            Screen.SpamRules.route -> SpamRulesScreen(
                viewModel = viewModel,
                modifier = modifier
            )
            Screen.Messages.route -> MessagesScreen(
                viewModel = viewModel,
                hasPermission = hasPermission,
                requestPermissions = requestPermissions,
                modifier = modifier
            )
            Screen.Recordings.route -> RecordingsScreen(
                modifier = modifier
            )
            Screen.RecorderSettings.route -> RecorderSettingsScreen(modifier = modifier)
            Screen.Settings.route -> SetupScreen(
                hasPermission = hasPermission,
                onRequestPermissions = requestPermissions,
                onOpenAppSettings = openAppSettings,
                isAccessibilityEnabled = isAccessibilityEnabled,
                onOpenAccessibility = openAccessibility,
                isBatteryOptimizationIgnored = isBatteryIgnored,
                onRequestBatteryOpt = requestBatteryOpt,
                onOpenBatterySettings = openBatterySettings,
                onContinue = { viewModel.selectRoute(Screen.CallLog.route) }
            )
            "notes" -> NotesScreen(
                viewModel = viewModel,
                modifier = modifier
            )
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            current = themeMode,
            onSelected = { viewModel.selectThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun ThemeDialog(
    current: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                ThemeMode.values().forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = { onSelected(mode) }
                        )
                        Text(
                            text = when (mode) {
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.SYSTEM -> "System"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
