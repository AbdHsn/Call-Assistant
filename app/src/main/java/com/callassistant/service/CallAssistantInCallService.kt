package com.callassistant.service

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.callassistant.InCallActivity
import com.callassistant.util.CallNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class CallState {
    object None : CallState()
    object Ended : CallState()
    data class Incoming(val number: String, val displayName: String?) : CallState()
    data class Active(val number: String, val displayName: String?) : CallState()
    data class Connecting(val number: String, val displayName: String?) : CallState()
}

class CallAssistantInCallService : InCallService() {

    companion object {
        @Volatile var activeCall: Call? = null
            private set

        @Volatile private var instance: CallAssistantInCallService? = null

        private val _callState = MutableStateFlow<CallState>(CallState.None)
        val callState: StateFlow<CallState> = _callState.asStateFlow()

        private val _callConnectTimestamp = MutableStateFlow<Long?>(null)
        val callConnectTimestamp: StateFlow<Long?> = _callConnectTimestamp.asStateFlow()

        private val _isMuted = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        private val _isSpeakerOn = MutableStateFlow(false)
        val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

        fun setMuted(muted: Boolean) {
            instance?.setMuted(muted)
        }

        fun setSpeakerOn(on: Boolean) {
            instance?.setAudioRoute(
                if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
            )
        }
    }

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
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        _isMuted.value = audioState.isMuted
        _isSpeakerOn.value = audioState.route == CallAudioState.ROUTE_SPEAKER
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCallNumber = ""
        currentCallName = null
        currentCallWasIncoming = false
        currentCallWasAnswered = false
        activeCall = call
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
        if (activeCall == call) {
            if (currentCallWasIncoming && !currentCallWasAnswered && currentCallNumber.isNotBlank()) {
                CallNotificationManager.showMissedCallNotification(this, currentCallNumber, currentCallName)
            }
            activeCall = null
            _callState.value = CallState.Ended
            _callConnectTimestamp.value = null
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
            else -> _callState.value
        }
        if (newState is CallState.Incoming) currentCallWasIncoming = true
        if (newState is CallState.Active) currentCallWasAnswered = true

        if (newState is CallState.Active && _callState.value !is CallState.Active) {
            _callConnectTimestamp.value = SystemClock.elapsedRealtime()
        } else if (newState !is CallState.Active) {
            _callConnectTimestamp.value = null
        }
        _callState.value = newState

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
