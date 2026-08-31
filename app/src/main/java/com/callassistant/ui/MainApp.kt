package com.callassistant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.R
import com.callassistant.ui.navigation.AppRoute
import com.callassistant.ui.navigation.mainBottomTabs
import com.callassistant.ui.navigation.settingsMenuRoutes
import com.callassistant.ui.notes.NotesViewModel
import com.callassistant.ui.phonebook.PhoneBookViewModel
import com.callassistant.ui.recordings.RecordingsViewModel
import com.callassistant.ui.screens.AboutScreen
import com.callassistant.ui.screens.CallLogScreen
import com.callassistant.ui.screens.ContactsScreen
import com.callassistant.ui.screens.DialPadScreen
import com.callassistant.ui.screens.MessagesScreen
import com.callassistant.ui.screens.NotesScreen
import com.callassistant.ui.screens.RecorderSettingsScreen
import com.callassistant.ui.screens.RecordingsScreen
import com.callassistant.ui.screens.SpamRulesScreen
import com.callassistant.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    requestPermissions: () -> Unit,
    hasPermission: (String) -> Boolean,
    isAccessibilityEnabled: () -> Boolean,
    isBatteryIgnored: () -> Boolean,
    requestBatteryOpt: () -> Unit,
    openBatterySettings: () -> Unit,
    openAppSettings: () -> Unit,
    openAccessibility: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    val phoneBookViewModel: PhoneBookViewModel = hiltViewModel()
    val messagesViewModel: MessagesViewModel = hiltViewModel()

    val openInAppMessage: (String) -> Unit = remember(messagesViewModel, viewModel) {
        { number ->
            messagesViewModel.openThread(number)
            viewModel.selectRoute(AppRoute.Messages)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = "Call Assistant",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Call Assistant", style = MaterialTheme.typography.titleMedium)
                    }
                },
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
                                    viewModel.selectRoute(AppRoute.Notes)
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
                                text = { Text("About") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.selectRoute(AppRoute.About)
                                }
                            )
                            settingsMenuRoutes.forEach { (route, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.selectRoute(route)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                mainBottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedRoute == tab.route,
                        onClick = { viewModel.selectRoute(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (selectedRoute) {
            AppRoute.Contacts -> {
                ContactsScreen(
                    viewModel = phoneBookViewModel,
                    hasPermission = hasPermission,
                    requestPermissions = requestPermissions,
                    onOpenMessage = openInAppMessage,
                    modifier = modifier
                )
            }
            AppRoute.CallLog -> {
                CallLogScreen(
                    viewModel = phoneBookViewModel,
                    hasPermission = hasPermission,
                    requestPermissions = requestPermissions,
                    onOpenMessage = openInAppMessage,
                    modifier = modifier
                )
            }
            AppRoute.DialPad -> {
                DialPadScreen(
                    viewModel = phoneBookViewModel,
                    hasPermission = hasPermission,
                    requestPermissions = requestPermissions,
                    onOpenMessage = openInAppMessage,
                    modifier = modifier
                )
            }
            AppRoute.Messages -> {
                MessagesScreen(
                    viewModel = messagesViewModel,
                    hasPermission = hasPermission,
                    requestPermissions = requestPermissions,
                    modifier = modifier
                )
            }
            AppRoute.SpamRules -> {
                val spamRulesViewModel: SpamRulesViewModel = hiltViewModel()
                SpamRulesScreen(
                    viewModel = spamRulesViewModel,
                    modifier = modifier
                )
            }
            AppRoute.Recordings -> {
                val recordingsViewModel: RecordingsViewModel = hiltViewModel()
                RecordingsScreen(
                    viewModel = recordingsViewModel,
                    modifier = modifier
                )
            }
            AppRoute.RecorderSettings -> RecorderSettingsScreen(modifier = modifier)
            AppRoute.Settings -> SetupScreen(
                hasPermission = hasPermission,
                onRequestPermissions = requestPermissions,
                onOpenAppSettings = openAppSettings,
                isAccessibilityEnabled = isAccessibilityEnabled,
                onOpenAccessibility = openAccessibility,
                isBatteryOptimizationIgnored = isBatteryIgnored,
                onRequestBatteryOpt = requestBatteryOpt,
                onOpenBatterySettings = openBatterySettings,
                onContinue = { viewModel.selectRoute(AppRoute.DialPad) }
            )
            AppRoute.Notes -> {
                val notesViewModel: NotesViewModel = hiltViewModel()
                NotesScreen(
                    viewModel = notesViewModel,
                    modifier = modifier
                )
            }
            AppRoute.About -> AboutScreen(modifier = modifier)
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
                ThemeMode.entries.forEach { mode ->
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
