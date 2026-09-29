package com.callassistant.incall

import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.callassistant.service.CallRecordingService
import com.callassistant.util.CallNotificationManager
import com.callassistant.util.CallRecordingPolicy
import com.callassistant.util.CallPlacer
import com.callassistant.util.IncomingCallUiHelper
import com.callassistant.util.MissedCallReadStore
import com.callassistant.util.UssdDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    private var inCallUiLaunched = false
    private var isCallForeground = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        session.muteHandler = { muted -> setMuted(muted) }
        session.audioRouteHandler = { route -> setAudioRoute(route) }
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
        inCallUiLaunched = false
        session.onCallAdded(call)
        call.registerCallback(callCallback)
        callAudioState?.let { session.onAudioStateChanged(it) }
        updateCallState(call)
    }

    private fun launchInCallActivity() {
        if (inCallUiLaunched) return
        inCallUiLaunched = true
        CallPlacer.launchInCallUi(this)
    }

    private fun presentIncomingCall(number: String, displayName: String?) {
        if (IncomingCallUiHelper.shouldLaunchInCallActivityDirectly(this)) {
            CallNotificationManager.cancelIncomingCallNotification(this)
            launchInCallActivity()
        } else {
            CallNotificationManager.showIncomingCallNotification(this, number, displayName)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (session.onCallRemoved(call)) {
            if (currentCallWasIncoming && !currentCallWasAnswered && currentCallNumber.isNotBlank()) {
                notifyMissedCall(currentCallNumber, currentCallName)
            }
            CallNotificationManager.cancelIncomingCallNotification(this)
            stopOngoingCallForeground()
            CallRecordingService.stop(this)
            currentCallNumber = ""
            currentCallName = null
            currentCallWasIncoming = false
            currentCallWasAnswered = false
            inCallUiLaunched = false
        }
    }

    private fun maybeStartAutoRecording(number: String, isIncoming: Boolean) {
        if (CallRecordingPolicy.shouldAutoRecord(this, number, isIncoming)) {
            CallRecordingService.start(this, number, isIncoming)
        }
    }

    private fun lookupContactName(number: String): String? {
        if (number.isBlank()) return null
        // Strip USSD/MMI characters before looking up; they are not part of a contact number
        val sanitized = number.filter { it.isDigit() || it == '+' }
        if (sanitized.isBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(sanitized)
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
        } catch (e: Exception) {
            null
        }
    }

    private fun updateCallState(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        
        // USSD/MMI codes are handled by the system dialog, not our in-call UI
        if (UssdDetector.isUssdCode(number)) {
            // Let the system handle USSD - don't show our UI
            return
        }
        
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
                presentIncomingCall(newState.number, newState.displayName)
                stopOngoingCallForeground()
            }
            is CallState.Active, is CallState.Connecting -> {
                launchInCallActivity()
                CallNotificationManager.cancelIncomingCallNotification(this)
                promoteOngoingCallForeground()
                if (newState is CallState.Active) {
                    maybeStartAutoRecording(currentCallNumber, currentCallWasIncoming)
                }
            }
            is CallState.Ended -> {
                CallNotificationManager.cancelIncomingCallNotification(this)
                stopOngoingCallForeground()
                CallRecordingService.stop(this)
            }
            else -> {}
        }
    }

    private fun promoteOngoingCallForeground() {
        if (!hasNotificationPermission()) return
        val notification = CallNotificationManager.buildOngoingCallNotification(
            this,
            currentCallNumber,
            currentCallName
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                CallNotificationManager.NOTIFICATION_ID_ONGOING_CALL,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(CallNotificationManager.NOTIFICATION_ID_ONGOING_CALL, notification)
        }
        isCallForeground = true
    }

    private fun stopOngoingCallForeground() {
        if (!isCallForeground) {
            CallNotificationManager.cancelOngoingCallNotification(this)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isCallForeground = false
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun notifyMissedCall(number: String, displayName: String?) {
        val timestamp = System.currentTimeMillis()
        MissedCallReadStore.recordMissedCall(this, number, displayName, timestamp)
        serviceScope.launch(Dispatchers.IO) {
            MissedCallReadStore.refreshNotification(this@CallAssistantInCallService)
        }
    }
}
