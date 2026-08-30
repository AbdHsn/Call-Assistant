package com.callassistant.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Required for this app to be eligible for the default SMS app role. Handles the
 * ACTION_RESPOND_VIA_MESSAGE intent (e.g. "quick response" when declining a call) by simply
 * stopping, since this app does not implement quick-reply-while-declining-a-call.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }
}
