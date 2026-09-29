package com.callassistant.ai.engine

interface LlmInferenceEngine {
    val isLoaded: Boolean
    suspend fun load(modelPath: String): Result<Unit>
    fun unload()
    suspend fun complete(prompt: String, maxTokens: Int = 120): Result<String>
    fun cancel()
}
