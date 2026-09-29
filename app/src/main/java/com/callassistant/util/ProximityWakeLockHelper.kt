package com.callassistant.util

import android.content.Context
import android.os.PowerManager

/**
 * Turns the screen off when the proximity sensor is covered (phone at the ear)
 * and back on when it is uncovered, using the standard in-call wake lock.
 */
class ProximityWakeLockHelper(context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    val isSupported: Boolean
        get() = powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)

    fun acquire() {
        if (wakeLock?.isHeld == true) return
        if (!isSupported) return
        wakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "CallAssistant:InCallProximity"
        ).apply {
            acquire()
        }
    }

    fun release() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
