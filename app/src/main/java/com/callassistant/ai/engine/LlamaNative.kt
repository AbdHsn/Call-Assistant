package com.callassistant.ai.engine

import android.util.Log

internal object LlamaNative {
    private const val TAG = "LlamaNative"
    private var libraryLoaded = false

    init {
        libraryLoaded = try {
            System.loadLibrary("callassistant_llama")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library not available", e)
            false
        }
    }

    fun isAvailable(): Boolean = libraryLoaded

    external fun nativeInit(nativeLibDir: String): Int
    external fun nativeLoad(modelPath: String): Int
    external fun nativeComplete(prompt: String, maxTokens: Int): String
    external fun nativeCancel()
    external fun nativeUnload()
    external fun nativeShutdown()
}
