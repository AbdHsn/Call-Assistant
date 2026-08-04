package com.callassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.callassistant.MainActivity
import com.callassistant.util.CallRecorder

class CallRecordingService : Service() {

    private val callRecorder by lazy { CallRecorder(this) }
    private var currentNumber: String = ""

    private var telephonyCallback: TelephonyCallback? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerCallStateListener()
        startForeground(NOTIFICATION_ID, buildNotification("Call recorder active"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentNumber = intent.getStringExtra(EXTRA_NUMBER) ?: currentNumber
                startForeground(NOTIFICATION_ID, buildNotification("Recording call..."))
                startRecording(currentNumber)
            }
            ACTION_SET_NUMBER -> {
                currentNumber = intent.getStringExtra(EXTRA_NUMBER) ?: currentNumber
            }
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRecording()
        unregisterCallStateListener()
        super.onDestroy()
    }

    private fun registerCallStateListener() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    when (state) {
                        TelephonyManager.CALL_STATE_OFFHOOK -> startRecording(currentNumber)
                        TelephonyManager.CALL_STATE_IDLE -> stopRecording()
                    }
                }
            }
            telephonyCallback = callback
            tm.registerTelephonyCallback(ContextCompat.getMainExecutor(this), callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    when (state) {
                        TelephonyManager.CALL_STATE_OFFHOOK -> startRecording(phoneNumber ?: currentNumber)
                        TelephonyManager.CALL_STATE_IDLE -> stopRecording()
                    }
                }
            }
            phoneStateListener = listener
            @Suppress("DEPRECATION")
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun unregisterCallStateListener() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { tm.unregisterTelephonyCallback(it) }
        }
        @Suppress("DEPRECATION")
        phoneStateListener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
    }

    private fun startRecording(number: String) {
        if (isRecording || !shouldRecord()) return
        val success = callRecorder.start(number)
        if (success) {
            isRecording = true
        } else {
            updateNotification("Call recording failed to start")
        }
    }

    private fun stopRecording() {
        if (!isRecording && !callRecorder.isRecording) return
        callRecorder.stop()
        isRecording = false
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun shouldRecord(): Boolean {
        val prefs = getSharedPreferences("recorder_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_record_enabled", true)
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
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
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
            .setContentTitle("Call Assistant")
            .setContentText(text)
            .setSmallIcon(com.callassistant.R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        private const val CHANNEL_ID = "call_recorder_channel"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRecording: Boolean = false
            private set

        const val ACTION_START = "com.callassistant.action.START_RECORDING"
        const val ACTION_STOP = "com.callassistant.action.STOP_RECORDING"
        const val ACTION_SET_NUMBER = "com.callassistant.action.SET_NUMBER"
        const val EXTRA_NUMBER = "number"

        fun start(context: Context, number: String = "") {
            val intent = Intent(context, CallRecordingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_NUMBER, number)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java)
                .setAction(ACTION_STOP)
            context.startService(intent)
        }

        fun setNumber(context: Context, number: String) {
            val intent = Intent(context, CallRecordingService::class.java)
                .setAction(ACTION_SET_NUMBER)
                .putExtra(EXTRA_NUMBER, number)
            context.startService(intent)
        }
    }
}
