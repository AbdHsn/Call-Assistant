package com.callassistant

import android.app.Application
import com.callassistant.data.db.AppDatabase
import com.callassistant.data.repository.SpamRuleRepository
import com.callassistant.util.CallNotificationManager
import org.osmdroid.config.Configuration

class CallAssistantApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val spamRuleRepository: SpamRuleRepository by lazy {
        SpamRuleRepository(database.spamRuleDao(), database.blockedNumberDao())
    }

    override fun onCreate() {
        super.onCreate()
        CallNotificationManager.createChannels(this)

        // Must be initialized before any osmdroid MapView is created, otherwise
        // tile requests silently fail (blank/grey map) due to missing cache dir/user-agent.
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
    }
}
