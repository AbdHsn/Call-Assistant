package com.callassistant.di

import com.callassistant.data.repository.CallLogRepository
import com.callassistant.data.repository.CallLogRepositoryImpl
import com.callassistant.data.repository.ContactRepository
import com.callassistant.data.repository.ContactRepositoryImpl
import com.callassistant.data.repository.NotesRepository
import com.callassistant.data.repository.NotesRepositoryImpl
import com.callassistant.data.repository.SettingsRepository
import com.callassistant.data.repository.SettingsRepositoryImpl
import com.callassistant.data.repository.SmsRepository
import com.callassistant.data.repository.SmsRepositoryImpl
import com.callassistant.data.repository.SpamRuleRepository
import com.callassistant.data.repository.SpamRuleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    @Binds
    @Singleton
    abstract fun bindCallLogRepository(impl: CallLogRepositoryImpl): CallLogRepository

    @Binds
    @Singleton
    abstract fun bindSmsRepository(impl: SmsRepositoryImpl): SmsRepository

    @Binds
    @Singleton
    abstract fun bindSpamRuleRepository(impl: SpamRuleRepositoryImpl): SpamRuleRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository
}
