package com.callassistant.ai.config

import com.callassistant.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzureOpenAiConfig @Inject constructor() {
    val endpoint: String = BuildConfig.AZURE_OPENAI_ENDPOINT.trim().trimEnd('/')
    val apiKey: String = BuildConfig.AZURE_OPENAI_API_KEY.trim()
    val deployment: String = BuildConfig.AZURE_OPENAI_MODEL.trim()
    val apiVersion: String = BuildConfig.AZURE_OPENAI_API_VERSION.trim()

    val isConfigured: Boolean =
        endpoint.isNotBlank() && apiKey.isNotBlank() && deployment.isNotBlank() && apiVersion.isNotBlank()
}
