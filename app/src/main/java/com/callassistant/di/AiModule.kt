package com.callassistant.di

import com.callassistant.ai.engine.LlmInferenceEngine
import com.callassistant.ai.engine.NativeLlamaInferenceEngine
import com.callassistant.data.repository.AiModelRepository
import com.callassistant.data.repository.AiModelRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiModelRepository(impl: AiModelRepositoryImpl): AiModelRepository

    companion object {
        @Provides
        @Singleton
        fun provideLlmInferenceEngine(native: NativeLlamaInferenceEngine): LlmInferenceEngine = native
    }
}
