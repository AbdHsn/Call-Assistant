package com.callassistant.di

import com.callassistant.data.repository.SmsRepository
import com.callassistant.data.repository.SpamRuleRepository
import com.callassistant.incall.CallSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun spamRuleRepository(): SpamRuleRepository
    fun smsRepository(): SmsRepository
    fun callSessionManager(): CallSessionManager
}
