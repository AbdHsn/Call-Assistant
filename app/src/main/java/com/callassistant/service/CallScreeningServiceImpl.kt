package com.callassistant.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.callassistant.data.repository.CallLogRepository
import com.callassistant.data.repository.SpamRuleRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class CallScreeningServiceImpl : CallScreeningService() {

    @Inject
    lateinit var repo: SpamRuleRepository

    @Inject
    lateinit var callLogRepository: CallLogRepository

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val number = handle?.schemeSpecificPart ?: return

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
                callLogRepository.markBlocked(number)
                repo.recordBlockedAttempt(number)
            }
        }
    }
}
