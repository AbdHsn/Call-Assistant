package com.callassistant.di

import com.callassistant.data.db.AppDatabase
import com.callassistant.data.repository.SpamRuleRepository
import com.callassistant.incall.CallSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun appDatabase(): AppDatabase
    fun spamRuleRepository(): SpamRuleRepository
    fun callSessionManager(): CallSessionManager
}
