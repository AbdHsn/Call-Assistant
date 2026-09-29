package com.callassistant.ai.engine

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class NativeLlamaInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : LlmInferenceEngine {

    private val mutex = Mutex()
    @Volatile private var loaded = false
    @Volatile private var backendInitialized = false

    override val isLoaded: Boolean get() = loaded

    val isNativeAvailable: Boolean get() = LlamaNative.isAvailable()

    override suspend fun load(modelPath: String): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!LlamaNative.isAvailable()) {
                return@withContext Result.failure(IllegalStateException("Native library unavailable"))
            }
            ensureBackendInitialized().onFailure { return@withContext Result.failure(it) }
            val code = LlamaNative.nativeLoad(modelPath)
            if (code != 0) {
                loaded = false
                Result.failure(IllegalStateException(loadErrorMessage(code)))
            } else {
                loaded = true
                Result.success(Unit)
            }
        }
    }

    override fun unload() {
        if (!LlamaNative.isAvailable()) return
        LlamaNative.nativeUnload()
        loaded = false
    }

    override suspend fun complete(prompt: String, maxTokens: Int): Result<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!loaded) {
                return@withContext Result.failure(IllegalStateException("Model not loaded"))
            }
            try {
                val text = LlamaNative.nativeComplete(prompt, maxTokens)
                Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun cancel() {
        if (LlamaNative.isAvailable()) {
            LlamaNative.nativeCancel()
        }
    }

    private fun ensureBackendInitialized(): Result<Unit> {
        if (backendInitialized) return Result.success(Unit)
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val code = LlamaNative.nativeInit(nativeDir)
        return if (code != 0) {
            Result.failure(IllegalStateException("Native backend init failed ($code)"))
        } else {
            backendInitialized = true
            Result.success(Unit)
        }
    }

    private fun loadErrorMessage(code: Int): String = when (code) {
        1 -> "Model file could not be loaded. Delete and re-download the model."
        2 -> "Not enough memory to run the model on this device."
        3 -> "Model sampler failed to initialize."
        4 -> "Downloaded file is not a valid GGUF model. Tap Retry download."
        5 -> "AI CPU backend failed to load. Reinstall the app."
        else -> "Native model load failed ($code)"
    }
}
