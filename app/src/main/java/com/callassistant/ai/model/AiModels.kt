package com.callassistant.ai.model

import com.callassistant.data.entity.SmsMessage

enum class AiContextScope(val label: String) {
    LAST_5("Last 5"),
    LAST_20("Last 20"),
    WHOLE_THREAD("Whole thread"),
    THIS_MESSAGE("This message only");

    companion object {
        val DEFAULT = LAST_5
    }
}

enum class SuggestionLanguage(val badge: String) {
    BN("BN"),
    EN("EN"),
    BL("BL")
}

enum class AiTone(val label: String, val promptHint: String) {
    FORMAL("Formal", "Rewrite more formally and politely"),
    CASUAL("Casual", "Rewrite in a casual, friendly tone"),
    SHORTER("Shorter", "Make it shorter, one brief line"),
    POLITE_NO("Polite no", "Rewrite as a gentle, polite decline")
}

data class AiSuggestion(
    val text: String,
    val language: SuggestionLanguage
)

data class MessageAiContext(
    val threadNumber: String,
    val contactName: String?,
    val messages: List<SmsMessage>,
    val scope: AiContextScope,
    val anchorMessage: SmsMessage? = null,
    val draftText: String = ""
)

sealed class AiModelStatus {
    data object NotDownloaded : AiModelStatus()
    data object UnsupportedDevice : AiModelStatus()
    data class Downloading(val progress: Float, val bytesDone: Long, val totalBytes: Long) : AiModelStatus()
    data class Ready(val path: String, val sizeBytes: Long, val variant: AiModelVariant) : AiModelStatus()
    data class Error(val message: String) : AiModelStatus()
}
