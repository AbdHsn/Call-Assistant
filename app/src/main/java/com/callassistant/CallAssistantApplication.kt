package com.callassistant

import android.app.Application
import com.callassistant.data.db.AppDatabase
import com.callassistant.data.repository.SpamRuleRepository

class CallAssistantApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val spamRuleRepository: SpamRuleRepository by lazy {
        SpamRuleRepository(database.spamRuleDao())
    }
}
