package com.callassistant.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun copyTextToClipboard(
    context: Context,
    text: String,
    label: String = "message",
    confirmationMessage: String = "Copied to clipboard"
) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, confirmationMessage, Toast.LENGTH_SHORT).show()
}
