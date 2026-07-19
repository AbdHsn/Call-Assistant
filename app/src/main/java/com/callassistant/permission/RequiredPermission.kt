package com.callassistant.permission

import android.Manifest

data class RequiredPermission(
    val permission: String,
    val title: String,
    val rationale: String
)

object Permissions {
    val contacts = RequiredPermission(
        Manifest.permission.READ_CONTACTS,
        "Contacts",
        "Contacts permission is needed to show caller names and sync your address book."
    )

    val callLog = RequiredPermission(
        Manifest.permission.READ_CALL_LOG,
        "Call Log",
        "Call log permission lets the app display recent calls and mark blocked attempts."
    )

    val sendSms = RequiredPermission(
        Manifest.permission.SEND_SMS,
        "Send SMS",
        "Send SMS permission is needed if you want to reply from this app."
    )

    val readSms = RequiredPermission(
        Manifest.permission.READ_SMS,
        "Read SMS",
        "Read SMS permission lets the app show your messages and check them for spam."
    )

    val receiveSms = RequiredPermission(
        Manifest.permission.RECEIVE_SMS,
        "Receive SMS",
        "Receive SMS permission lets the app detect incoming messages in real time."
    )

    val callPhone = RequiredPermission(
        Manifest.permission.CALL_PHONE,
        "Call Phone",
        "Call phone permission lets you dial numbers from the custom dial pad."
    )

    fun all(): List<RequiredPermission> = listOf(
        contacts, callLog, sendSms, readSms, receiveSms, callPhone
    )
}
