package com.callassistant.util

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.callassistant.R

/**
 * Notification icons: [smallIconRes] for the status bar, [largeIconBitmap] for the expanded shade.
 */
object AppNotificationIcons {

    /** Launcher foreground — works as a notification small icon (status bar). */
    val smallIconRes: Int = R.drawable.ic_launcher_foreground

    fun largeIconBitmap(context: Context) =
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_app_logo)

    fun NotificationCompat.Builder.applyAppIcons(context: Context): NotificationCompat.Builder =
        setSmallIcon(smallIconRes).setLargeIcon(largeIconBitmap(context))
}
