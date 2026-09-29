package com.callassistant.ai.engine

import com.callassistant.ai.config.AzureOpenAiConfig
import com.callassistant.ai.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AzureOpenAiInferenceEngine @Inject constructor(
    private val config: AzureOpenAiConfig
) : LlmInferenceEngine {

    private val client = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override val isLoaded: Boolean get() = config.isConfigured

    override suspend fun load(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (config.isConfigured) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Azure OpenAI is not configured in local.properties"))
        }
    }

    override fun unload() = Unit

    override suspend fun complete(prompt: String, maxTokens: Int): Result<String> =
        completeChat(listOf(ChatMessage("user", prompt)), maxTokens)

    suspend fun completeChat(messages: List<ChatMessage>, maxTokens: Int): Result<String> =
        withContext(Dispatchers.IO) {
            if (!config.isConfigured) {
                return@withContext Result.failure(
                    IllegalStateException("Azure OpenAI is not configured")
                )
            }
            if (messages.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No messages to send"))
            }

            val messagesJson = JSONArray().apply {
                messages.forEach { message ->
                    put(
                        JSONObject()
                            .put("role", message.role)
                            .put("content", message.content)
                    )
                }
            }

            val payload = JSONObject()
                .put("messages", messagesJson)
                .put("max_completion_tokens", maxTokens)

            val url =
                "${config.endpoint}/openai/deployments/${config.deployment}/chat/completions" +
                    "?api-version=${config.apiVersion}"

            val request = Request.Builder()
                .url(url)
                .addHeader("api-key", config.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException(parseErrorMessage(response.code, bodyText))
                        )
                    }
                    val json = JSONObject(bodyText)
                    val content = json
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    Result.success(content)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun cancel() = Unit

    private fun parseErrorMessage(code: Int, body: String): String {
        if (body.isBlank()) return "Azure OpenAI request failed (HTTP $code)"
        return try {
            val json = JSONObject(body)
            val message = json.optJSONObject("error")?.optString("message").orEmpty()
            if (message.isNotBlank()) "Azure OpenAI: $message" else "Azure OpenAI request failed (HTTP $code)"
        } catch (_: Exception) {
            "Azure OpenAI request failed (HTTP $code)"
        }
    }
}
