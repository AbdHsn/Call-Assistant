package com.callassistant.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.callassistant.MainActivity
import com.callassistant.util.AppNotificationIcons.applyAppIcons

class CallReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_NUMBER = "extra_number"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_PLACE_CALL = "place_call_number"
        private const val CHANNEL_ID = "call_reminders"
        private const val CHANNEL_NAME = "Call Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra(EXTRA_NUMBER) ?: return
        val name = intent.getStringExtra(EXTRA_NAME)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val callIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PLACE_CALL, number)
        }
        val callPendingIntent = PendingIntent.getActivity(
            context,
            number.hashCode(),
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            number.hashCode() + 1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .applyAppIcons(context)
            .setContentTitle("Call back reminder")
            .setContentText("Call back ${name ?: number}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.sym_action_call, "Call", callPendingIntent)
            .build()

        notificationManager.notify(number.hashCode(), notification)
    }
}
