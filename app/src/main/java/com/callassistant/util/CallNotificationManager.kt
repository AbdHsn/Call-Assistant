package com.callassistant.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.net.Uri
import com.callassistant.incall.InCallActivity
import com.callassistant.MainActivity
import com.callassistant.R
import com.callassistant.util.AppNotificationIcons.applyAppIcons
import com.callassistant.sms.SmsActivity
import com.callassistant.incall.CallActionReceiver

/**
 * Builds and manages the incoming call notification used by the default-dialer [InCallService].
 *
 * Because this app is the default phone app, the system expects us to present our own incoming
 * call UI both as a full-screen activity and as a heads-up notification. This notification uses
 * the app icon as the caller/brand logo and provides Answer and Decline actions directly on it.
 */
object CallNotificationManager {

    const val EXTRA_OPEN_CALL_LOG = "open_call_log"

    private const val CHANNEL_INCOMING = "call_assistant_incoming_call"
    private const val CHANNEL_MISSED = "call_assistant_missed_call_v2"
    private const val CHANNEL_SMS = "call_assistant_sms"
    private const val CHANNEL_ONGOING_CALL = "call_assistant_ongoing_call_v2"

    private const val NOTIFICATION_ID_INCOMING = 1
    const val NOTIFICATION_ID_ONGOING_CALL = 2
    private const val NOTIFICATION_ID_MISSED_CALLS = 3

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
                setShowBadge(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SMS, "Messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "New message notifications"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING_CALL, "Ongoing calls", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tap to return to the current call"
                setSound(null, null)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
        )
        AiModelDownloadNotifier.ensureChannel(context)
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
        val fullScreenPendingIntent = inCallPendingIntent(context, NOTIFICATION_ID_INCOMING, fullScreenIntent)

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

        val notification = NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .applyAppIcons(context)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(fullScreenPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.sym_action_call, "Answer", answerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_INCOMING, notification)
    }

    fun cancelIncomingCallNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_INCOMING)
    }

    fun buildOngoingCallNotification(
        context: Context,
        number: String,
        displayName: String?
    ): Notification {
        createChannels(context)

        val openCallPendingIntent = inCallPendingIntent(context, NOTIFICATION_ID_ONGOING_CALL)

        val title = displayName?.ifBlank { null } ?: number.ifBlank { "Call" }
        val subtitle = when {
            number.isNotBlank() -> "Tap to return to call · $number"
            else -> "Tap to return to call"
        }

        return NotificationCompat.Builder(context, CHANNEL_ONGOING_CALL)
            .applyAppIcons(context)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(openCallPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun showOngoingCallNotification(
        context: Context,
        number: String,
        displayName: String?
    ) {
        if (!context.hasNotificationPermission()) return
        val notification = buildOngoingCallNotification(context, number, displayName)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ONGOING_CALL, notification)
    }

    fun cancelOngoingCallNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ONGOING_CALL)
    }

    fun refreshMissedCallNotification(
        context: Context,
        unseen: List<UnseenMissedCall>
    ) {
        if (!context.hasNotificationPermission() || unseen.isEmpty()) return
        createChannels(context)

        val latest = unseen.first()
        val count = unseen.size

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CALL_LOG, true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MISSED_CALLS,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callBackIntent = Intent(context, CallActionReceiver::class.java)
            .setAction(CallActionReceiver.ACTION_CALL_BACK)
            .putExtra(CallActionReceiver.EXTRA_NUMBER, latest.number)
        val callBackPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_MISSED_CALLS + 1,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sendMessageIntent = Intent(context, SmsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SmsActivity.EXTRA_NUMBER, latest.number)
        }
        val sendMessagePendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MISSED_CALLS + 2,
            sendMessageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val latestTitle = latest.displayName?.ifBlank { null }
            ?: latest.number.ifBlank { "Unknown caller" }
        val title = if (count == 1) latestTitle else "$count missed calls"
        val text = when {
            count == 1 && latest.number.isNotBlank() && latest.displayName?.isNotBlank() == true ->
                "Missed call · ${latest.number}"
            count == 1 -> "Missed call"
            else -> "Tap to view call log"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .applyAppIcons(context)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setNumber(count)
            .addAction(android.R.drawable.sym_action_call, "Call back", callBackPendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "Send message", sendMessagePendingIntent)

        if (count > 1) {
            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
            unseen.take(5).forEach { call ->
                val lineTitle = call.displayName?.ifBlank { null } ?: call.number.ifBlank { "Unknown" }
                inboxStyle.addLine(lineTitle)
            }
            if (count > 5) {
                inboxStyle.setSummaryText("+${count - 5} more")
            }
            builder.setStyle(inboxStyle)
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MISSED_CALLS, builder.build())
    }

    fun cancelMissedCallNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_MISSED_CALLS)
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

        val notification = NotificationCompat.Builder(context, CHANNEL_SMS)
            .applyAppIcons(context)
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

    private fun inCallPendingIntent(
        context: Context,
        requestCode: Int,
        intent: Intent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun Context.hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }
}
