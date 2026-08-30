package com.callassistant.di

import android.content.Context
import com.callassistant.data.db.AppDatabase
import com.callassistant.data.repository.SpamRuleRepository
import com.callassistant.data.sync.CallLogSyncer
import com.callassistant.data.sync.ContactSyncer
import com.callassistant.data.sync.SmsSyncer
import com.callassistant.incall.CallSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideSpamRuleRepository(db: AppDatabase): SpamRuleRepository =
        SpamRuleRepository(db.spamRuleDao(), db.blockedNumberDao())

    @Provides
    @Singleton
    fun provideCallSessionManager(): CallSessionManager = CallSessionManager()

    @Provides
    fun provideContactSyncer(@ApplicationContext context: Context): ContactSyncer =
        ContactSyncer(context)

    @Provides
    fun provideCallLogSyncer(@ApplicationContext context: Context): CallLogSyncer =
        CallLogSyncer(context)

    @Provides
    fun provideSmsSyncer(@ApplicationContext context: Context): SmsSyncer =
        SmsSyncer(context)
}
