package com.callassistant.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import com.callassistant.MainActivity
import com.callassistant.util.AppNotificationIcons.applyAppIcons

object AiModelDownloadNotifier {

    private const val CHANNEL_ID = "call_assistant_ai_model_download"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "AI model downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progress while downloading offline AI models"
                setShowBadge(false)
            }
        )
    }

    fun showProgress(
        context: Context,
        modelName: String,
        progressPercent: Int,
        bytesDone: Long,
        totalBytes: Long
    ) {
        if (!context.hasNotificationPermission()) return
        ensureChannel(context)
        val indeterminate = totalBytes <= 0L
        val max = 100
        val progress = if (indeterminate) 0 else progressPercent.coerceIn(0, max)
        val contentText = if (indeterminate) {
            "Starting download…"
        } else {
            "${formatSize(bytesDone)} / ${formatSize(totalBytes)} · $progressPercent%"
        }

        val notification = buildNotification(context, modelName)
            .setContentText(contentText)
            .setProgress(max, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showComplete(context: Context, modelName: String) {
        if (!context.hasNotificationPermission()) return
        ensureChannel(context)
        val notification = buildNotification(context, modelName)
            .setContentText("Download complete — ready to use")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showFailed(context: Context, modelName: String, message: String) {
        if (!context.hasNotificationPermission()) return
        ensureChannel(context)
        val notification = buildNotification(context, modelName)
            .setContentText(message.ifBlank { "Download failed" })
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showCancelled(context: Context, modelName: String) {
        dismiss(context)
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun foregroundInfo(
        context: Context,
        modelName: String,
        progressPercent: Int,
        bytesDone: Long,
        totalBytes: Long
    ): ForegroundInfo {
        ensureChannel(context)
        val indeterminate = totalBytes <= 0L
        val max = 100
        val progress = if (indeterminate) 0 else progressPercent.coerceIn(0, max)
        val contentText = if (indeterminate) {
            "Starting download…"
        } else {
            "${formatSize(bytesDone)} / ${formatSize(totalBytes)} · $progressPercent%"
        }

        val notification = buildNotification(context, modelName)
            .setContentText(contentText)
            .setProgress(max, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(context: Context, modelName: String): NotificationCompat.Builder {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .applyAppIcons(context)
            .setContentTitle("Downloading $modelName")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024.0) else String.format("%.0f MB", mb)
    }

    private fun Context.hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
