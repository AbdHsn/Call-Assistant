package com.callassistant.util

/**
 * In-memory snapshot of the active call recording session for in-call UI.
 */
object CallRecordingState {

    @Volatile
    var isRecording: Boolean = false
        private set

    @Volatile
    var isAutoRecording: Boolean = false
        private set

    @Volatile
    var startedAtMillis: Long = 0L
        private set

    @Volatile
    var lastFailureMessage: String? = null
        private set

    fun onStarted(auto: Boolean) {
        isRecording = true
        isAutoRecording = auto
        startedAtMillis = System.currentTimeMillis()
        lastFailureMessage = null
    }

    fun onStopped() {
        isRecording = false
        isAutoRecording = false
        startedAtMillis = 0L
    }

    fun onFailed(message: String) {
        isRecording = false
        isAutoRecording = false
        startedAtMillis = 0L
        lastFailureMessage = message
    }
}
