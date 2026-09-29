package com.callassistant.ai.prompt

import com.callassistant.ai.model.AiContextScope
import com.callassistant.ai.model.AiTone
import com.callassistant.ai.model.ChatMessage
import com.callassistant.ai.model.MessageAiContext
import com.callassistant.ai.model.SuggestionLanguage
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagePromptBuilder @Inject constructor() {

    fun buildSuggestionMessages(
        context: MessageAiContext,
        customInstruction: String? = null
    ): List<ChatMessage> {
        val scopedMessages = selectMessages(context)
        val history = formatHistory(scopedMessages)
        val contact = context.contactName ?: context.threadNumber
        val instruction = customInstruction?.trim().orEmpty()

        val userPrompt = buildString {
            if (contact != "new-message") {
                appendLine("Contact: $contact")
            } else {
                appendLine("Compose a new SMS message.")
            }
            if (history.isNotBlank()) {
                appendLine("Conversation:")
                appendLine(history)
            }
            if (context.draftText.isNotBlank()) {
                appendLine("Draft so far: ${context.draftText}")
            }
            if (instruction.isNotBlank()) {
                appendLine("Instruction: $instruction")
                appendLine("Write one short SMS reply.")
            } else {
                appendLine(
                    "Suggest exactly 3 brief reply options — one Bangla [BN], one English [EN], " +
                        "one Banglish [BL]. One per line, prefix each with [BN], [EN], or [BL]."
                )
            }
        }.trim()

        return listOf(
            ChatMessage(
                role = "system",
                content =
                    "You write short SMS replies for a phone user in Bangladesh. " +
                        "Reply in Bangla, English, or Banglish as appropriate. " +
                        "Output ONLY message text. No quotes or explanation."
            ),
            ChatMessage(role = "user", content = userPrompt)
        )
    }

    fun buildToneMessages(
        text: String,
        tone: AiTone,
        language: SuggestionLanguage
    ): List<ChatMessage> = listOf(
        ChatMessage(
            role = "system",
            content = "Rewrite SMS text. Output ONLY the rewritten message, nothing else."
        ),
        ChatMessage(
            role = "user",
            content = buildString {
                appendLine("Language: ${language.badge}")
                appendLine("Tone: ${tone.promptHint}")
                appendLine("Text: $text")
            }.trim()
        )
    )

    fun buildSuggestionPrompt(context: MessageAiContext, customInstruction: String? = null): String {
        val scopedMessages = selectMessages(context)
        val history = formatHistory(scopedMessages)
        val contact = context.contactName ?: context.threadNumber
        val instruction = customInstruction?.trim().orEmpty()

        return buildString {
            appendLine("<|im_start|>system")
            appendLine(
                "You write short SMS replies for a phone user in Bangladesh. " +
                    "Reply in Bangla, English, or Banglish as appropriate."
            )
            appendLine("Output ONLY message text. No quotes or explanation.")
            appendLine("")
            appendLine("<|im_start|>user")
            if (contact != "new-message") {
                appendLine("Contact: $contact")
            } else {
                appendLine("Compose a new SMS message.")
            }
            if (history.isNotBlank()) {
                appendLine("Conversation:")
                appendLine(history)
            }
            if (context.draftText.isNotBlank()) {
                appendLine("Draft so far: ${context.draftText}")
            }
            if (instruction.isNotBlank()) {
                appendLine("Instruction: $instruction")
                appendLine("Write one short SMS reply.")
            } else {
                appendLine(
                    "Suggest exactly 3 brief reply options — one Bangla [BN], one English [EN], " +
                        "one Banglish [BL]. One per line, prefix each with [BN], [EN], or [BL]."
                )
            }
            appendLine("")
            appendLine("<|im_start|>assistant")
        }
    }

    fun buildTonePrompt(text: String, tone: AiTone, language: SuggestionLanguage): String {
        return buildString {
            appendLine("<|im_start|>system")
            appendLine("Rewrite SMS text. Output ONLY the rewritten message, nothing else.")
            appendLine("")
            appendLine("<|im_start|>user")
            appendLine("Language: ${language.badge}")
            appendLine("Tone: ${tone.promptHint}")
            appendLine("Text: $text")
            appendLine("")
            appendLine("<|im_start|>assistant")
        }
    }

    private fun selectMessages(context: MessageAiContext): List<SmsMessage> {
        val sorted = context.messages.sortedBy { it.timestamp }
        return when (context.scope) {
            AiContextScope.THIS_MESSAGE -> {
                val anchor = context.anchorMessage ?: sorted.lastOrNull()
                if (anchor == null) emptyList() else {
                    val index = sorted.indexOfFirst { it.id == anchor.id && it.timestamp == anchor.timestamp }
                    if (index < 0) listOf(anchor) else sorted.subList(maxOf(0, index - 1), index + 1)
                }
            }
            AiContextScope.LAST_5 -> sorted.takeLast(5)
            AiContextScope.LAST_20 -> sorted.takeLast(20)
            AiContextScope.WHOLE_THREAD -> sorted.takeLast(40)
        }
    }

    private fun formatHistory(messages: List<SmsMessage>): String {
        if (messages.isEmpty()) return ""
        return messages.joinToString("\n") { message ->
            val speaker = if (message.direction == SmsDirection.OUT) "Me" else "Them"
            "$speaker: ${message.body.replace("\n", " ")}"
        }
    }

    fun parseSuggestions(raw: String): List<Pair<SuggestionLanguage, String>> {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val parsed = lines.mapNotNull { line ->
            val match = LANGUAGE_PREFIX.find(line)
            if (match != null) {
                val lang = when (match.groupValues[1].uppercase()) {
                    "BN" -> SuggestionLanguage.BN
                    "EN" -> SuggestionLanguage.EN
                    "BL" -> SuggestionLanguage.BL
                    else -> null
                }
                val text = line.removePrefix(match.value).trim().trim('"')
                if (lang != null && text.isNotBlank()) lang to text else null
            } else {
                null
            }
        }
        return parsed
    }

    fun parseSingleReply(raw: String): String =
        raw.trim().lines().firstOrNull { it.isNotBlank() }?.trim('"').orEmpty()

    private companion object {
        val LANGUAGE_PREFIX = Regex("^\\[(BN|EN|BL)\\]\\s*", RegexOption.IGNORE_CASE)
    }
}
