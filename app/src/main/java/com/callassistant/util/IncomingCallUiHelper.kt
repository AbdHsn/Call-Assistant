package com.callassistant.util

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

/**
 * Decides whether to show the full-screen in-call activity directly or rely on the
 * high-priority incoming-call notification (with full-screen intent when the device is locked).
 */
object IncomingCallUiHelper {

    fun shouldLaunchInCallActivityDirectly(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return true
        val keyguardManager = context.getSystemService(KeyguardManager::class.java) ?: return true
        return powerManager.isInteractive && !keyguardManager.isKeyguardLocked
    }
}
