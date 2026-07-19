package com.callassistant.keyboard

import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import com.callassistant.R

class CustomKeyboardService : InputMethodService() {

    companion object {
        const val PREFS_NAME = "keyboard_prefs"
        const val KEY_BACKGROUND_URI = "background_uri"
    }

    private lateinit var keyboardView: View
    private lateinit var backgroundView: ImageView

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard, null)
        backgroundView = keyboardView.findViewById(R.id.keyboard_background)
        loadBackgroundImage()
        bindKeys()
        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadBackgroundImage()
    }

    private fun loadBackgroundImage() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val uriString = prefs.getString(KEY_BACKGROUND_URI, null) ?: return
        try {
            backgroundView.setImageURI(Uri.parse(uriString))
        } catch (_: Exception) {
            backgroundView.setImageDrawable(null)
        }
    }

    private fun bindKeys() {
        val container = keyboardView.findViewById<LinearLayout>(R.id.keyboard_container)
        bindButtonsRecursively(container)
    }

    private fun bindButtonsRecursively(view: View) {
        if (view is Button) {
            when (view.id) {
                R.id.key_backspace -> view.setOnClickListener { handleBackspace() }
                R.id.key_space -> view.setOnClickListener { commitText(" ") }
                R.id.key_enter -> view.setOnClickListener { handleEnter() }
                else -> {
                    val text = view.text.toString()
                    if (text.length == 1) {
                        view.setOnClickListener { commitText(text) }
                    }
                }
            }
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                bindButtonsRecursively(view.getChildAt(i))
            }
        }
    }

    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun handleBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun handleEnter() {
        currentInputConnection?.let {
            val action = currentInputEditorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            if (action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_ACTION_GO) {
                it.performEditorAction(action)
            } else {
                commitText("\n")
            }
        }
    }
}
