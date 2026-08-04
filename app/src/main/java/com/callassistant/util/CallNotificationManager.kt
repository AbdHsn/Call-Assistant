package com.callassistant.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.net.Uri
import com.callassistant.InCallActivity
import com.callassistant.MainActivity
import com.callassistant.R
import com.callassistant.SmsActivity
import com.callassistant.receiver.CallActionReceiver

/**
 * Builds and manages the incoming call notification used by the default-dialer [InCallService].
 *
 * Because this app is the default phone app, the system expects us to present our own incoming
 * call UI both as a full-screen activity and as a heads-up notification. This notification uses
 * the app icon as the caller/brand logo and provides Answer and Decline actions directly on it.
 */
object CallNotificationManager {

    private const val CHANNEL_INCOMING = "call_assistant_incoming_call"
    private const val CHANNEL_MISSED = "call_assistant_missed_call"
    private const val CHANNEL_SMS = "call_assistant_sms"
    private const val CHANNEL_ONGOING_CALL = "call_assistant_ongoing_call"

    private const val NOTIFICATION_ID_INCOMING = 1
    private const val NOTIFICATION_ID_ONGOING_CALL = 2

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_INCOMING, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "High-priority heads-up notification for incoming calls"
                setSound(null, null)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MISSED, "Missed calls", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Missed call reminders"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SMS, "Messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "New message notifications"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING_CALL, "Ongoing calls", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Tap to return to the current call"
                setSound(null, null)
            }
        )
    }

    fun showIncomingCallNotification(
        context: Context,
        number: String,
        displayName: String?
    ) {
        if (!context.hasNotificationPermission()) return
        createChannels(context)

        val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("number", number)
            putExtra("displayName", displayName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(context, CallActionReceiver::class.java)
            .setAction(CallActionReceiver.ACTION_ANSWER_CALL)
        val answerPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, CallActionReceiver::class.java)
            .setAction(CallActionReceiver.ACTION_DECLINE_CALL)
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = displayName?.ifBlank { null } ?: number.ifBlank { "Unknown caller" }
        val text = if (displayName?.isNotBlank() == true && number.isNotBlank()) number else "Incoming call"

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.mipmap.ic_launcher, "Answer", answerPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Decline", declinePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_INCOMING, notification)
    }

    fun cancelIncomingCallNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_INCOMING)
    }

    fun showOngoingCallNotification(
        context: Context,
        number: String,
        displayName: String?
    ) {
        if (!context.hasNotificationPermission()) return
        createChannels(context)

        val openCallIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openCallPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = displayName?.ifBlank { null } ?: number.ifBlank { "Call" }

        val notification = NotificationCompat.Builder(context, CHANNEL_ONGOING_CALL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(number.ifBlank { "Ongoing call" })
            .setContentIntent(openCallPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ONGOING_CALL, notification)
    }

    fun cancelOngoingCallNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ONGOING_CALL)
    }

    fun showMissedCallNotification(
        context: Context,
        number: String,
        displayName: String?
    ) {
        if (!context.hasNotificationPermission()) return
        createChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callBackIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${number}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val callBackPendingIntent = PendingIntent.getActivity(
            context,
            1,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sendMessageIntent = Intent(context, SmsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SmsActivity.EXTRA_NUMBER, number)
        }
        val sendMessagePendingIntent = PendingIntent.getActivity(
            context,
            2,
            sendMessageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = displayName?.ifBlank { null } ?: number.ifBlank { "Unknown caller" }
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText("Missed call")
            .setContentIntent(openAppPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .addAction(R.mipmap.ic_launcher, "Call back", callBackPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Send message", sendMessagePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(numberNotificationId(number), notification)
    }

    fun showSmsNotification(
        context: Context,
        number: String,
        displayName: String?,
        body: String
    ) {
        if (!context.hasNotificationPermission()) return
        createChannels(context)

        val openThreadIntent = Intent(context, SmsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SmsActivity.EXTRA_NUMBER, number)
        }
        val openThreadPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openThreadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = displayName?.ifBlank { null } ?: number.ifBlank { "Unknown sender" }
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, CHANNEL_SMS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openThreadPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(numberNotificationId(number), notification)
    }

    private fun numberNotificationId(number: String): Int = number.hashCode() and 0x7fffffff

    private fun Context.hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }
}
