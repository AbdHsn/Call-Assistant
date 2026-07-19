package com.callassistant.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callassistant.ui.components.KeyboardSettingsDialog
import com.callassistant.ui.screens.CallLogScreen
import com.callassistant.ui.screens.ContactsScreen
import com.callassistant.ui.screens.DialPadScreen
import com.callassistant.ui.screens.MessagesScreen
import com.callassistant.ui.screens.SpamRulesScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Contacts : Screen("contacts", "Contacts", Icons.Default.Person)
    object CallLog : Screen("call_log", "Call Log", Icons.Default.Call)
    object DialPad : Screen("dial_pad", "Dial", Icons.Default.Dialpad)
    object SpamRules : Screen("spam_rules", "Spam", Icons.Default.Shield)
    object Messages : Screen("messages", "Messages", Icons.AutoMirrored.Filled.Message)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    factory: MainViewModelFactory,
    requestPermissions: () -> Unit,
    hasPermission: (String) -> Boolean,
    viewModel: MainViewModel = viewModel(factory = factory)
) {
    val screens = listOf(Screen.Contacts, Screen.CallLog, Screen.DialPad, Screen.SpamRules, Screen.Messages)
    val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
    var showKeyboardSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Assistant") },
                actions = {
                    IconButton(onClick = { showKeyboardSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Keyboard settings")
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
        }
    }

    if (showKeyboardSettings) {
        KeyboardSettingsDialog(onDismiss = { showKeyboardSettings = false })
    }
}
