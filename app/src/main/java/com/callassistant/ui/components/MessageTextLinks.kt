package com.callassistant.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.callassistant.util.telCallUri

private enum class MessageLinkType {
    Url,
    Phone
}

private data class MessageLinkMatch(
    val type: MessageLinkType,
    val start: Int,
    val end: Int,
    val value: String
)

private const val URL_ANNOTATION_TAG = "message_url"
private const val PHONE_ANNOTATION_TAG = "message_phone"

private object MessageTextLinkifier {
    fun findLinks(text: String): List<MessageLinkMatch> {
        if (text.isBlank()) return emptyList()

        val matches = mutableListOf<MessageLinkMatch>()

        val urlMatcher = Patterns.WEB_URL.matcher(text)
        while (urlMatcher.find()) {
            val value = urlMatcher.group().orEmpty()
            if (value.isNotBlank()) {
                matches += MessageLinkMatch(
                    type = MessageLinkType.Url,
                    start = urlMatcher.start(),
                    end = urlMatcher.end(),
                    value = value
                )
            }
        }

        val phoneMatcher = Patterns.PHONE.matcher(text)
        while (phoneMatcher.find()) {
            val value = phoneMatcher.group().orEmpty()
            if (value.isBlank() || value.count { it.isDigit() } < 7) continue
            val start = phoneMatcher.start()
            val end = phoneMatcher.end()
            if (matches.any { it.type == MessageLinkType.Url && rangesOverlap(it.start, it.end, start, end) }) {
                continue
            }
            matches += MessageLinkMatch(
                type = MessageLinkType.Phone,
                start = start,
                end = end,
                value = value
            )
        }

        return matches.sortedBy { it.start }
    }

    fun buildAnnotatedString(
        text: String,
        linkColor: Color
    ): AnnotatedString {
        val matches = findLinks(text)
        if (matches.isEmpty()) return AnnotatedString(text)

        val linkStyle = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline
        )

        return buildAnnotatedString {
            var lastIndex = 0
            for (match in matches) {
                if (match.start > lastIndex) {
                    append(text.substring(lastIndex, match.start))
                }
                val segmentStart = length
                append(text.substring(match.start, match.end))
                val segmentEnd = length
                addStyle(linkStyle, segmentStart, segmentEnd)
                when (match.type) {
                    MessageLinkType.Url -> {
                        addStringAnnotation(
                            tag = URL_ANNOTATION_TAG,
                            annotation = normalizeUrl(match.value),
                            start = segmentStart,
                            end = segmentEnd
                        )
                    }
                    MessageLinkType.Phone -> {
                        addStringAnnotation(
                            tag = PHONE_ANNOTATION_TAG,
                            annotation = match.value,
                            start = segmentStart,
                            end = segmentEnd
                        )
                    }
                }
                lastIndex = match.end
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    private fun rangesOverlap(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean {
        return aStart < bEnd && bStart < aEnd
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("ftp://", ignoreCase = true) -> trimmed
            trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
            else -> "https://$trimmed"
        }
    }
}

internal fun openMessagePhoneLink(context: android.content.Context, phone: String) {
    val dialNumber = phone.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
    if (dialNumber.isBlank()) return
    context.startActivity(Intent(Intent.ACTION_DIAL, telCallUri(dialNumber)))
}

internal fun openMessageUrlLink(
    context: android.content.Context,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    url: String
) {
    runCatching { uriHandler.openUri(url) }
        .onFailure {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
}

@Composable
fun LinkifiedMessageText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onNonLinkClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val links = remember(text) { MessageTextLinkifier.findLinks(text) }
    val annotated = remember(text, linkColor) {
        MessageTextLinkifier.buildAnnotatedString(text, linkColor)
    }

    if (links.isEmpty()) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow
        )
        return
    }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
        onClick = { offset ->
            annotated.getStringAnnotations(URL_ANNOTATION_TAG, offset, offset)
                .firstOrNull()
                ?.let { openMessageUrlLink(context, uriHandler, it.item) }
                ?: annotated.getStringAnnotations(PHONE_ANNOTATION_TAG, offset, offset)
                    .firstOrNull()
                    ?.let { openMessagePhoneLink(context, it.item) }
                ?: onNonLinkClick?.invoke()
        }
    )
}
