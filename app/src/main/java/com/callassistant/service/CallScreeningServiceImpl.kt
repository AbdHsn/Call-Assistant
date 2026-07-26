package com.callassistant.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.callassistant.CallAssistantApplication
import com.callassistant.data.repository.SpamRuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val number = handle?.schemeSpecificPart ?: return

        val app = application as CallAssistantApplication
        val repo: SpamRuleRepository = app.spamRuleRepository

        val isBlocked = runBlocking(Dispatchers.IO) {
            repo.isNumberBlocked(number)
        }

        val response = CallResponse.Builder()
            .setDisallowCall(isBlocked)
            .setRejectCall(isBlocked)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)

        if (isBlocked) {
            CoroutineScope(Dispatchers.IO).launch {
                app.database.callLogDao().markBlocked(number)
                repo.recordBlockedAttempt(number)
            }
        }
    }
}
