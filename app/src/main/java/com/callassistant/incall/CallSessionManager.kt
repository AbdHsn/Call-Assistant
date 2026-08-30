package com.callassistant.incall

import android.os.SystemClock
import android.telecom.Call
import android.telecom.CallAudioState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CallSessionManager {

    @Volatile
    var activeCall: Call? = null
        private set

    private val _callState = MutableStateFlow<CallState>(CallState.None)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _callConnectTimestamp = MutableStateFlow<Long?>(null)
    val callConnectTimestamp: StateFlow<Long?> = _callConnectTimestamp.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    internal var muteHandler: ((Boolean) -> Unit)? = null
    internal var audioRouteHandler: ((Boolean) -> Unit)? = null

    fun setMuted(muted: Boolean) {
        muteHandler?.invoke(muted)
    }

    fun setSpeakerOn(on: Boolean) {
        audioRouteHandler?.invoke(on)
    }

    fun answer() {
        activeCall?.answer(0)
    }

    fun reject() {
        activeCall?.reject(false, null)
    }

    fun hangUp() {
        activeCall?.disconnect()
    }

    fun sendDtmf(digit: Char) {
        activeCall?.let {
            it.playDtmfTone(digit)
            it.stopDtmfTone()
        }
    }

    internal fun onCallAdded(call: Call) {
        activeCall = call
    }

    internal fun onCallRemoved(call: Call): Boolean {
        if (activeCall != call) return false
        activeCall = null
        _callState.value = CallState.Ended
        _callConnectTimestamp.value = null
        return true
    }

    internal fun onAudioStateChanged(audioState: CallAudioState) {
        _isMuted.value = audioState.isMuted
        _isSpeakerOn.value = audioState.route == CallAudioState.ROUTE_SPEAKER
    }

    internal fun currentState(): CallState = _callState.value

    internal fun updateCallState(newState: CallState) {
        if (newState is CallState.Active && _callState.value !is CallState.Active) {
            _callConnectTimestamp.value = SystemClock.elapsedRealtime()
        } else if (newState !is CallState.Active) {
            _callConnectTimestamp.value = null
        }
        _callState.value = newState
    }
}
