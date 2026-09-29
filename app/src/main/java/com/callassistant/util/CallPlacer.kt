package com.callassistant.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.callassistant.di.AppEntryPoint
import com.callassistant.incall.InCallActivity
import android.Manifest
import dagger.hilt.android.EntryPointAccessors

enum class CallPlaceResult {
    Success,
    UssdSent,
    InvalidNumber,
    NeedPermission,
    NeedDefaultDialer,
    Failed
}

object CallPlacer {

    /**
     * Places a call through Telecom so [com.callassistant.incall.CallAssistantInCallService]
     * owns the session and shows [InCallActivity]. Requires Call Assistant to be the default
     * phone app — [Intent.ACTION_CALL] is intentionally not used because it opens the stock dialer UI.
     */
    fun placeCall(context: Context, number: String): CallPlaceResult {
        if (number.isBlank()) return CallPlaceResult.InvalidNumber

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return CallPlaceResult.NeedPermission
        }

        if (!DefaultDialerUtils.isDefaultDialer(context)) {
            return CallPlaceResult.NeedDefaultDialer
        }

        val telecom = context.getSystemService(TelecomManager::class.java)
            ?: return CallPlaceResult.Failed

        // USSD/MMI codes are handled by the system, not as regular calls
        if (UssdDetector.isUssdCode(number)) {
            return try {
                telecom.placeCall(telCallUri(number), Bundle())
                CallPlaceResult.UssdSent
            } catch (e: SecurityException) {
                CallPlaceResult.Failed
            }
        }

        return try {
            prepareSessionForNewCall(context)
            telecom.placeCall(telCallUri(number), Bundle())
            CallNotificationManager.showOngoingCallNotification(context, number, null)
            launchInCallUi(context)
            CallPlaceResult.Success
        } catch (e: SecurityException) {
            CallPlaceResult.Failed
        }
    }

    fun placeCallWithFeedback(context: Context, number: String): CallPlaceResult {
        val result = placeCall(context, number)
        when (result) {
            CallPlaceResult.NeedDefaultDialer -> {
                Toast.makeText(
                    context,
                    "Choose Call Assistant as your default phone app (not the same as permissions)",
                    Toast.LENGTH_LONG
                ).show()
                openDefaultDialerRequest(context)
            }
            CallPlaceResult.NeedPermission -> Toast.makeText(
                context,
                "Phone permission is required to place calls",
                Toast.LENGTH_SHORT
            ).show()
            CallPlaceResult.Failed -> Toast.makeText(
                context,
                "Could not place call",
                Toast.LENGTH_SHORT
            ).show()
            else -> Unit
        }
        return result
    }

    fun launchInCallUi(context: Context) {
        val intent = Intent(context, InCallActivity::class.java).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }
        context.startActivity(intent)
    }

    fun openDefaultDialerRequest(context: Context) {
        DefaultDialerUtils.createRequestIntent(context)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun prepareSessionForNewCall(context: Context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppEntryPoint::class.java
        ).callSessionManager().prepareForNewCall()
    }
}
