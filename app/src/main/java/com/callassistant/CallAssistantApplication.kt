package com.callassistant

import android.app.Application
import com.callassistant.data.repository.SpamRuleRepository
import com.callassistant.util.CallNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

@HiltAndroidApp
class CallAssistantApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var spamRuleRepository: SpamRuleRepository

    @Inject
    lateinit var aiModelRepository: com.callassistant.data.repository.AiModelRepository

    override fun onCreate() {
        super.onCreate()
        CallNotificationManager.createChannels(this)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        applicationScope.launch {
            spamRuleRepository.seedDefaultsIfEmpty()
        }

        applicationScope.launch {
            aiModelRepository.refreshStatus()
            aiModelRepository.scheduleSilentDownloadIfNeeded()
        }
    }
}
