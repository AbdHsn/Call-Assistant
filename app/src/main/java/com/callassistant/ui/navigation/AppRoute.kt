package com.callassistant.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

/** Typed destinations for the single-activity shell. */
sealed class AppRoute(val route: String) {
    data object Contacts : AppRoute("contacts")
    data object CallLog : AppRoute("call_log")
    data object DialPad : AppRoute("dial_pad")
    data object Messages : AppRoute("messages")
    data object SpamRules : AppRoute("spam_rules")
    data object Recordings : AppRoute("recordings")
    data object RecorderSettings : AppRoute("recorder_settings")
    data object Notes : AppRoute("notes")
    data object About : AppRoute("about")
    data object AiSettings : AppRoute("ai_settings")
    data object Settings : AppRoute("settings")

    val isBottomTab: Boolean
        get() = this in bottomTabs

    companion object {
        val bottomTabs = listOf(CallLog, Contacts, DialPad, Messages)

        fun fromRoute(route: String): AppRoute = when (route) {
            Contacts.route -> Contacts
            CallLog.route -> CallLog
            DialPad.route -> DialPad
            Messages.route -> Messages
            SpamRules.route -> SpamRules
            Recordings.route -> Recordings
            RecorderSettings.route -> RecorderSettings
            Notes.route -> Notes
            About.route -> About
            AiSettings.route -> AiSettings
            Settings.route -> Settings
            else -> DialPad
        }
    }
}

data class BottomTab(
    val route: AppRoute,
    val title: String,
    val icon: ImageVector
)

val mainBottomTabs = listOf(
    BottomTab(AppRoute.CallLog, "Call Log", Icons.Default.Call),
    BottomTab(AppRoute.Contacts, "Contacts", Icons.Default.Person),
    BottomTab(AppRoute.DialPad, "Dial", Icons.Default.Dialpad),
    BottomTab(AppRoute.Messages, "Msg", Icons.AutoMirrored.Filled.Message)
)

val settingsMenuRoutes = listOf(
    AppRoute.Notes to "Notes",
    AppRoute.SpamRules to "Spam & Block Numbers",
    AppRoute.Recordings to "Records",
    AppRoute.RecorderSettings to "Recorder settings",
    AppRoute.AiSettings to "AI Assistant",
    AppRoute.Settings to "Settings"
)
