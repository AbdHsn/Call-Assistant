package com.callassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.callassistant.MainActivity
import com.callassistant.util.AppNotificationIcons.applyAppIcons
import com.callassistant.util.CallRecorder
import com.callassistant.util.CallRecordingPolicy
import com.callassistant.util.CallRecordingState

class CallRecordingService : Service() {

    private val callRecorder by lazy { CallRecorder(this) }
    private var currentNumber: String = ""
    private var currentIsIncoming: Boolean = false
    private var isForegroundActive: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentNumber = intent.getStringExtra(EXTRA_NUMBER) ?: currentNumber
                currentIsIncoming = intent.getBooleanExtra(EXTRA_INCOMING, false)
                val manual = intent.getBooleanExtra(EXTRA_MANUAL, false)
                val requireBootstrap = intent.getBooleanExtra(EXTRA_REQUIRE_FGS_BOOTSTRAP, false)
                if (requireBootstrap) {
                    bootstrapForeground()
                }
                startRecording(currentNumber, currentIsIncoming, manual)
            }
            ACTION_SET_NUMBER -> {
                currentNumber = intent.getStringExtra(EXTRA_NUMBER) ?: currentNumber
            }
            ACTION_STOP -> {
                if (!isRecording && !callRecorder.isRecording) {
                    shutdownIfIdle()
                } else {
                    stopRecording()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRecordingInternal(stopService = false)
        super.onDestroy()
    }

    private fun startRecording(number: String, isIncoming: Boolean, manual: Boolean) {
        if (isRecording || callRecorder.isRecording) return
        if (!manual && !CallRecordingPolicy.shouldAutoRecord(this, number, isIncoming)) {
            shutdownIfIdle()
            return
        }
        if (!CallRecordingPolicy.hasRecordAudioPermission(this)) {
            CallRecordingState.onFailed("Microphone permission required")
            shutdownIfIdle()
            return
        }

        val success = callRecorder.start(number)
        if (success) {
            isRecording = true
            CallRecordingState.onStarted(auto = !manual)
            ensureForeground(
                if (manual) {
                    "Recording call — tap Stop in call screen to end"
                } else {
                    "Auto-recording call — tap Stop in call screen to end"
                }
            )
        } else {
            Log.w(TAG, "Call recording failed to start for $number")
            CallRecordingState.onFailed("Could not start recording — try speakerphone")
            shutdownIfIdle()
        }
    }

    private fun stopRecording() {
        stopRecordingInternal(stopService = true)
    }

    private fun stopRecordingInternal(stopService: Boolean) {
        if (callRecorder.isRecording) {
            callRecorder.stop()
        }
        isRecording = false
        CallRecordingState.onStopped()
        if (isForegroundActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            isForegroundActive = false
        }
        if (stopService) stopSelf()
    }

    private fun shutdownIfIdle() {
        if (isForegroundActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            isForegroundActive = false
        }
        if (!isRecording && !callRecorder.isRecording) {
            stopSelf()
        }
    }

    /** Satisfies the startForegroundService deadline without opening a microphone AppOp. */
    private fun bootstrapForeground() {
        if (isForegroundActive) return
        val notification = buildNotification("Preparing recording...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
        isForegroundActive = true
    }

    private fun ensureForeground(text: String) {
        val notification = buildNotification(text)
        if (!isForegroundActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundActive = true
        } else {
            getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for ongoing call recording"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .applyAppIcons(this)
            .setContentTitle("Call Assistant")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val TAG = "CallRecordingService"
        private const val CHANNEL_ID = "call_recorder_channel"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRecording: Boolean = false
            private set

        const val ACTION_START = "com.callassistant.action.START_RECORDING"
        const val ACTION_STOP = "com.callassistant.action.STOP_RECORDING"
        const val ACTION_SET_NUMBER = "com.callassistant.action.SET_NUMBER"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_INCOMING = "is_incoming"
        const val EXTRA_MANUAL = "manual"
        const val EXTRA_REQUIRE_FGS_BOOTSTRAP = "require_fgs_bootstrap"

        fun start(
            context: Context,
            number: String = "",
            isIncoming: Boolean = false,
            manual: Boolean = false
        ) {
            if (!CallRecordingPolicy.hasRecordAudioPermission(context)) return
            val intent = Intent(context, CallRecordingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_NUMBER, number)
                .putExtra(EXTRA_INCOMING, isIncoming)
                .putExtra(EXTRA_MANUAL, manual)
            try {
                // During an active in-call UI session a normal service start is enough and
                // avoids opening a microphone AppOp before MediaRecorder is running.
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start call recording service", e)
            }
        }

        fun startFromBackground(
            context: Context,
            number: String = "",
            isIncoming: Boolean = false
        ) {
            if (!CallRecordingPolicy.hasRecordAudioPermission(context)) return
            val intent = Intent(context, CallRecordingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_NUMBER, number)
                .putExtra(EXTRA_INCOMING, isIncoming)
                .putExtra(EXTRA_MANUAL, false)
                .putExtra(EXTRA_REQUIRE_FGS_BOOTSTRAP, true)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start call recording service from background", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java)
                .setAction(ACTION_STOP)
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to stop call recording service", e)
            }
        }

        fun setNumber(context: Context, number: String) {
            context.getSharedPreferences("recorder_settings", Context.MODE_PRIVATE)
                .edit()
                .putString(PENDING_NUMBER_KEY, number)
                .apply()
        }

        private const val PENDING_NUMBER_KEY = "pending_call_number"
    }
}
