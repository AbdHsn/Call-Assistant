package com.callassistant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callassistant.R
import com.callassistant.ui.navigation.AppRoute
import com.callassistant.ui.navigation.mainBottomTabs
import com.callassistant.ui.navigation.settingsMenuRoutes
import com.callassistant.ui.messageai.MessageAiViewModel
import com.callassistant.ui.notes.NotesViewModel
import com.callassistant.ui.phonebook.PhoneBookViewModel
import com.callassistant.ui.recordings.RecordingsViewModel
import com.callassistant.ui.screens.AiSettingsScreen
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
import com.callassistant.ui.util.ResponsiveScreenContainer
import com.callassistant.ui.util.RetainedTab
import com.callassistant.ui.util.ScreenWidthClass
import com.callassistant.ui.util.rememberScreenWidthClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    requestPermissions: () -> Unit,
    hasPermission: (String) -> Boolean,
    isDefaultDialer: () -> Boolean,
    onRequestDefaultDialer: () -> Unit,
    isAccessibilityEnabled: () -> Boolean,
    isBatteryIgnored: () -> Boolean,
    requestBatteryOpt: () -> Unit,
    openBatterySettings: () -> Unit,
    openAppSettings: () -> Unit,
    openAccessibility: () -> Unit,
    openCallLogFromNotification: Boolean = false,
    onOpenCallLogHandled: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var messagesOverlayActive by remember { mutableStateOf(false) }
    val phoneBookViewModel: PhoneBookViewModel = hiltViewModel()
    val phoneBookState by phoneBookViewModel.uiState.collectAsStateWithLifecycle()
    val messagesViewModel: MessagesViewModel = hiltViewModel()
    val messageAiViewModel: MessageAiViewModel = hiltViewModel()

    LaunchedEffect(openCallLogFromNotification) {
        if (openCallLogFromNotification) {
            viewModel.selectRoute(AppRoute.CallLog)
            phoneBookViewModel.markAllMissedCallsSeen()
            onOpenCallLogHandled()
        }
    }

    val openInAppMessage: (String) -> Unit = remember(messagesViewModel, viewModel) {
        { number ->
            messagesViewModel.openThread(number)
            viewModel.selectRoute(AppRoute.Messages)
        }
    }

    androidx.compose.runtime.LaunchedEffect(selectedRoute) {
        if (selectedRoute != AppRoute.Messages) {
            messagesOverlayActive = false
        }
    }

    val widthClass = rememberScreenWidthClass()
    val useNavigationRail = widthClass != ScreenWidthClass.Compact

    val mainRouteContent: @Composable (Modifier) -> Unit = { routeModifier ->
        Box(modifier = routeModifier.fillMaxSize()) {
            RetainedTab(visible = selectedRoute == AppRoute.Contacts) {
                ResponsiveScreenContainer(modifier = Modifier.fillMaxSize()) {
                    ContactsScreen(
                        viewModel = phoneBookViewModel,
                        hasPermission = hasPermission,
                        requestPermissions = requestPermissions,
                        onOpenMessage = openInAppMessage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            RetainedTab(visible = selectedRoute == AppRoute.CallLog) {
                ResponsiveScreenContainer(modifier = Modifier.fillMaxSize()) {
                    CallLogScreen(
                        viewModel = phoneBookViewModel,
                        hasPermission = hasPermission,
                        requestPermissions = requestPermissions,
                        onOpenMessage = openInAppMessage,
                        onOpenDialPad = { viewModel.selectRoute(AppRoute.DialPad) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            RetainedTab(visible = selectedRoute == AppRoute.DialPad) {
                DialPadScreen(
                    viewModel = phoneBookViewModel,
                    hasPermission = hasPermission,
                    requestPermissions = requestPermissions,
                    onOpenMessage = openInAppMessage,
                    modifier = Modifier.fillMaxSize()
                )
            }
            RetainedTab(visible = selectedRoute == AppRoute.Messages) {
                MessagesScreen(
                    viewModel = messagesViewModel,
                    aiViewModel = messageAiViewModel,
                    hasPermission = hasPermission,
                    requestPermissions = requestPermissions,
                    onOpenAiSettings = { viewModel.selectRoute(AppRoute.AiSettings) },
                    onOverlayActiveChange = { messagesOverlayActive = it },
                    modifier = Modifier.fillMaxSize()
                )
            }

            when (selectedRoute) {
                AppRoute.Contacts,
                AppRoute.CallLog,
                AppRoute.DialPad,
                AppRoute.Messages -> Unit
                AppRoute.SpamRules -> {
                    val spamRulesViewModel: SpamRulesViewModel = hiltViewModel()
                    Box(Modifier.fillMaxSize().zIndex(2f)) {
                        ResponsiveScreenContainer(modifier = Modifier.fillMaxSize()) {
                            SpamRulesScreen(
                                viewModel = spamRulesViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                AppRoute.Recordings -> {
                    val recordingsViewModel: RecordingsViewModel = hiltViewModel()
                    Box(Modifier.fillMaxSize().zIndex(2f)) {
                        ResponsiveScreenContainer(modifier = Modifier.fillMaxSize()) {
                            RecordingsScreen(
                                viewModel = recordingsViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                AppRoute.RecorderSettings -> {
                    Box(Modifier.fillMaxSize().zIndex(2f)) {
                        ResponsiveScreenContainer(modifier = Modifier.fillMaxSize()) {
                            RecorderSettingsScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                AppRoute.Settings -> {
                    Box(Modifier.fillMaxSize().zIndex(2f)) {
                        ResponsiveScreenContainer(modifier = Modifier.fillMaxSize()) {
                            SetupScreen(
                                hasPermission = hasPermission,
                                onRequestPermissions = requestPermissions,
                                onOpenAppSettings = openAppSettings,
                                isDefaultDialer = isDefaultDialer,
                                onRequestDefaultDialer = onRequestDefaultDialer,
                                isAccessibilityEnabled = isAccessibilityEnabled,
                                onOpenAccessibility = openAccessibility,
                                isBatteryOptimizationIgnored = isBatteryIgnored,
                                onRequestBatteryOpt = requestBatteryOpt,
                                onOpenBatterySettings = openBatterySettings,
                                onContinue = { viewModel.selectRoute(AppRoute.DialPad) }
                            )
                        }
                    }
                }
                AppRoute.Notes -> {
                    val notesViewModel: NotesViewModel = hiltViewModel()
                    Box(Modifier.fillMaxSize().zIndex(2f)) {
                        ResponsiveScreenContainer(modifier = Modifier.fillMaxSize()) {
                            NotesScreen(
                                viewModel = notesViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                AppRoute.About -> {
                    Box(Modifier.fillMaxSize().zIndex(2f)) {
                        ResponsiveScreenContainer(modifier = Modifier.fillMaxSize()) {
                            AboutScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                AppRoute.AiSettings -> {
                    Box(Modifier.fillMaxSize().zIndex(2f)) {
                        ResponsiveScreenContainer(modifier = Modifier.fillMaxSize()) {
                            AiSettingsScreen(
                                viewModel = messageAiViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    val topBar: @Composable () -> Unit = {
        if (!(selectedRoute == AppRoute.Messages && messagesOverlayActive)) {
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
        }
    }

    if (useNavigationRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(modifier = Modifier.fillMaxHeight()) {
                mainBottomTabs.forEach { tab ->
                    NavigationRailItem(
                        selected = selectedRoute == tab.route,
                        onClick = { viewModel.selectRoute(tab.route) },
                        icon = {
                            MainTabIcon(
                                tab = tab,
                                unseenMissedCount = if (tab.route == AppRoute.CallLog) {
                                    phoneBookState.unseenMissedCount
                                } else {
                                    0
                                }
                            )
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
            Scaffold(
                topBar = topBar
            ) { padding ->
                mainRouteContent(Modifier.padding(padding))
            }
        }
    } else {
        Scaffold(
            topBar = topBar,
            bottomBar = {
                NavigationBar {
                    mainBottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedRoute == tab.route,
                            onClick = { viewModel.selectRoute(tab.route) },
                            icon = {
                                MainTabIcon(
                                    tab = tab,
                                    unseenMissedCount = if (tab.route == AppRoute.CallLog) {
                                        phoneBookState.unseenMissedCount
                                    } else {
                                        0
                                    }
                                )
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { padding ->
            mainRouteContent(Modifier.padding(padding))
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
private fun MainTabIcon(
    tab: com.callassistant.ui.navigation.BottomTab,
    unseenMissedCount: Int
) {
    if (unseenMissedCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(
                        text = if (unseenMissedCount > 99) "99+" else unseenMissedCount.toString()
                    )
                }
            }
        ) {
            Icon(tab.icon, contentDescription = tab.title)
        }
    } else {
        Icon(tab.icon, contentDescription = tab.title)
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
