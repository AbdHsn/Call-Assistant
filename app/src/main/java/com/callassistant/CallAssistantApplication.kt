package com.callassistant

import android.app.Application
import com.callassistant.data.db.AppDatabase
import com.callassistant.data.entity.RuleType
import com.callassistant.data.entity.SpamRule
import com.callassistant.util.CallNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

@HiltAndroidApp
class CallAssistantApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        CallNotificationManager.createChannels(this)

        // Must be initialized before any osmdroid MapView is created, otherwise
        // tile requests silently fail (blank/grey map) due to missing cache dir/user-agent.
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        seedSpamRules()
    }

    private fun seedSpamRules() {
        applicationScope.launch {
            val dao = database.spamRuleDao()
            if (dao.getAll().first().isNotEmpty()) return@launch
            // Blocking rules: numbers/messages matching these are auto-rejected.
            dao.insert(
                SpamRule(
                    pattern = "+1900",
                    type = RuleType.PREFIX,
                    label = "Premium/scam prefix",
                    isBlocking = true
                )
            )
            dao.insert(
                SpamRule(
                    pattern = "^(\\d)\\1{6,}$",
                    type = RuleType.REGEX,
                    label = "Repeated-digit spoofed number",
                    isBlocking = true
                )
            )
            dao.insert(
                SpamRule(
                    pattern = "^0*123456789\$|^0*987654321\$",
                    type = RuleType.REGEX,
                    label = "Sequential-digit spoofed number",
                    isBlocking = true
                )
            )
            dao.insert(
                SpamRule(
                    pattern = "000000000",
                    type = RuleType.EXACT,
                    label = "Known spam number",
                    isBlocking = true
                )
            )
            // Flag-only rules: surfaced to the user but calls/messages are still allowed through.
            dao.insert(
                SpamRule(
                    pattern = "^\\+?880?1[3-9]\\d{8}$",
                    type = RuleType.REGEX,
                    label = "Unverified BD mobile pattern",
                    isBlocking = false
                )
            )
            dao.insert(
                SpamRule(
                    pattern = "you have won",
                    type = RuleType.PREFIX,
                    label = "Prize/lottery scam wording",
                    isBlocking = false
                )
            )
        }
    }
}
