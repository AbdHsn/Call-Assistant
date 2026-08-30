package com.callassistant.incall

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.callassistant.util.CallNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class CallState {
    object None : CallState()
    object Ended : CallState()
    data class Incoming(val number: String, val displayName: String?) : CallState()
    data class Active(val number: String, val displayName: String?) : CallState()
    data class Connecting(val number: String, val displayName: String?) : CallState()
}

@AndroidEntryPoint
class CallAssistantInCallService : InCallService() {

    @Inject
    lateinit var session: CallSessionManager

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) { updateCallState(call) }
        override fun onDetailsChanged(call: Call, details: Call.Details) { updateCallState(call) }
    }

    private var currentCallNumber = ""
    private var currentCallName: String? = null
    private var currentCallWasIncoming = false
    private var currentCallWasAnswered = false

    override fun onCreate() {
        super.onCreate()
        session.muteHandler = { muted -> setMuted(muted) }
        session.audioRouteHandler = { on ->
            setAudioRoute(if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session.muteHandler = null
        session.audioRouteHandler = null
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        session.onAudioStateChanged(audioState)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCallNumber = ""
        currentCallName = null
        currentCallWasIncoming = false
        currentCallWasAnswered = false
        session.onCallAdded(call)
        call.registerCallback(callCallback)
        updateCallState(call)
        startActivity(
            Intent(this, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }
        )
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (session.onCallRemoved(call)) {
            if (currentCallWasIncoming && !currentCallWasAnswered && currentCallNumber.isNotBlank()) {
                CallNotificationManager.showMissedCallNotification(this, currentCallNumber, currentCallName)
            }
            CallNotificationManager.cancelIncomingCallNotification(this)
            CallNotificationManager.cancelOngoingCallNotification(this)
            currentCallNumber = ""
            currentCallName = null
            currentCallWasIncoming = false
            currentCallWasAnswered = false
        }
    }

    private fun lookupContactName(number: String): String? {
        if (number.isBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
                } else null
            }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun updateCallState(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val name = call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: lookupContactName(number)
        currentCallNumber = number
        currentCallName = name

        val newState = when (call.state) {
            Call.STATE_RINGING -> CallState.Incoming(number, name)
            Call.STATE_ACTIVE -> CallState.Active(number, name)
            Call.STATE_DIALING, Call.STATE_CONNECTING -> CallState.Connecting(number, name)
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallState.Ended
            else -> session.currentState()
        }
        if (newState is CallState.Incoming) currentCallWasIncoming = true
        if (newState is CallState.Active) currentCallWasAnswered = true

        session.updateCallState(newState)

        when (newState) {
            is CallState.Incoming -> {
                CallNotificationManager.showIncomingCallNotification(
                    this,
                    newState.number,
                    newState.displayName
                )
                CallNotificationManager.cancelOngoingCallNotification(this)
            }
            is CallState.Active, is CallState.Connecting -> {
                CallNotificationManager.cancelIncomingCallNotification(this)
                CallNotificationManager.showOngoingCallNotification(
                    this,
                    currentCallNumber,
                    currentCallName
                )
            }
            is CallState.Ended -> {
                CallNotificationManager.cancelIncomingCallNotification(this)
                CallNotificationManager.cancelOngoingCallNotification(this)
            }
            else -> {}
        }
    }
}
