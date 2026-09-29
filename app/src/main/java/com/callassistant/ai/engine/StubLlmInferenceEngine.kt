package com.callassistant.ai.engine

import com.callassistant.ai.model.AiTone
import com.callassistant.ai.model.MessageAiContext
import com.callassistant.ai.model.SuggestionLanguage
import com.callassistant.ai.prompt.MessagePromptBuilder
import com.callassistant.data.entity.SmsDirection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Template-based engine until llama.cpp JNI is wired.
 * Uses conversation context to produce BN / EN / Banglish suggestions offline.
 */
@Singleton
class StubLlmInferenceEngine @Inject constructor(
    private val promptBuilder: MessagePromptBuilder
) : LlmInferenceEngine {

    private val mutex = Mutex()
    @Volatile private var loaded = false
    @Volatile private var cancelled = false

    override val isLoaded: Boolean get() = loaded

    override suspend fun load(modelPath: String): Result<Unit> = mutex.withLock {
        delay(300)
        loaded = true
        Result.success(Unit)
    }

    override fun unload() {
        loaded = false
    }

    override fun cancel() {
        cancelled = true
    }

    override suspend fun complete(prompt: String, maxTokens: Int): Result<String> = mutex.withLock {
        cancelled = false
        delay(600)
        if (cancelled) return Result.failure(CancelledException())
        Result.success("")
    }

    suspend fun generateSuggestions(context: MessageAiContext, customInstruction: String? = null): List<Pair<SuggestionLanguage, String>> {
        delay(if (loaded) 400 else 200)
        if (cancelled) return emptyList()

        val lastIncoming = context.messages
            .sortedBy { it.timestamp }
            .lastOrNull { it.direction == SmsDirection.IN }

        val anchor = context.anchorMessage ?: lastIncoming
        val body = anchor?.body?.lowercase().orEmpty()

        if (!customInstruction.isNullOrBlank()) {
            val reply = buildCustomReply(customInstruction, body)
            return listOf(SuggestionLanguage.EN to reply)
        }

        return buildDefaultSuggestions(body)
    }

    suspend fun refineTone(text: String, tone: AiTone, language: SuggestionLanguage): String {
        delay(350)
        if (cancelled) return text
        return when (tone) {
            AiTone.FORMAL -> when (language) {
                SuggestionLanguage.BN -> "ধন্যবাদ, আমি পরে আপনাকে জানাব।"
                SuggestionLanguage.BL -> "Thanks bhai, ami pore janabo."
                SuggestionLanguage.EN -> "Thank you. I will get back to you shortly."
            }
            AiTone.CASUAL -> when (language) {
                SuggestionLanguage.BN -> "ঠিক আছে, দেখা হচ্ছে!"
                SuggestionLanguage.BL -> "Ok bhai, dekha hocche!"
                SuggestionLanguage.EN -> "Sure, sounds good!"
            }
            AiTone.SHORTER -> when (language) {
                SuggestionLanguage.BN -> "ঠিক আছে।"
                SuggestionLanguage.BL -> "Ok."
                SuggestionLanguage.EN -> "OK."
            }
            AiTone.POLITE_NO -> when (language) {
                SuggestionLanguage.BN -> "দুঃখিত, এখন সম্ভব হচ্ছে না।"
                SuggestionLanguage.BL -> "Sorry bhai, akhon possible na."
                SuggestionLanguage.EN -> "Sorry, I can't make it right now."
            }
        }.let { refined ->
            if (text.length <= 20) refined else "$refined"
        }
    }

    private fun buildCustomReply(instruction: String, lastBody: String): String {
        val lower = instruction.lowercase()
        return when {
            "bangla" in lower || "bengali" in lower -> "ঠিক আছে, আমি জানিয়ে দিচ্ছি।"
            "no" in lower || "decline" in lower -> "Sorry, I won't be able to make it."
            "yes" in lower || "confirm" in lower -> "Yes, confirmed. See you then."
            lastBody.contains("?") -> "Let me check and get back to you."
            else -> "Thanks for the message. I'll reply soon."
        }
    }

    private fun buildDefaultSuggestions(lastBody: String): List<Pair<SuggestionLanguage, String>> {
        val lower = lastBody.lowercase()
        return when {
            lower.contains("?") && (lower.contains("kobe") || lower.contains("when") || lower.contains("kal")) -> listOf(
                SuggestionLanguage.BN to "হ্যাঁ, ঠিক আছে।",
                SuggestionLanguage.EN to "Yes, that works for me.",
                SuggestionLanguage.BL to "Ha bhai, thik ache."
            )
            lower.contains("?") -> listOf(
                SuggestionLanguage.BN to "আমি দেখে জানাচ্ছি।",
                SuggestionLanguage.EN to "Let me check and confirm.",
                SuggestionLanguage.BL to "Dekhe confirm korchi."
            )
            lower.contains("thanks") || lower.contains("thank") || lower.contains("dhonnobad") -> listOf(
                SuggestionLanguage.BN to "আপনাকেও ধন্যবাদ!",
                SuggestionLanguage.EN to "You're welcome!",
                SuggestionLanguage.BL to "Welcome bhai!"
            )
            lower.contains("call") || lower.contains("phone") -> listOf(
                SuggestionLanguage.BN to "ঠিক আছে, আমি পরে কল করব।",
                SuggestionLanguage.EN to "Sure, I'll call you back later.",
                SuggestionLanguage.BL to "Ok, pore call korbo."
            )
            else -> listOf(
                SuggestionLanguage.BN to "ঠিক আছে, বুঝেছি।",
                SuggestionLanguage.EN to "Got it, thanks!",
                SuggestionLanguage.BL to "Ok bhai, bujhlam."
            )
        }
    }

    class CancelledException : Exception("Generation cancelled")
}
