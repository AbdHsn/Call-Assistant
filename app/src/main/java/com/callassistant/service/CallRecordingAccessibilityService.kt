package com.callassistant.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.telephony.TelephonyManager
import android.view.accessibility.AccessibilityEvent

class CallRecordingAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val packageName = event.packageName?.toString() ?: return
        if (!isDialerPackage(packageName)) return

        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (tm.callState == TelephonyManager.CALL_STATE_OFFHOOK && !CallRecordingService.isRecording) {
            CallRecordingService.start(this, "")
        }
    }

    override fun onInterrupt() {}

    private fun isDialerPackage(pkg: String): Boolean {
        val known = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.huawei.android.dialer",
            "com.xiaomi.dialer",
            "com.oppo.dialer",
            "com.vivo.dialer",
            "com.oneplus.dialer"
        )
        return pkg.contains("dialer", ignoreCase = true) || known.contains(pkg)
    }
}
