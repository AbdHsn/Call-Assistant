package com.callassistant.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.callassistant.ai.model.AiModelCatalog
import com.callassistant.ai.model.AiModelDefinition
import com.callassistant.ai.model.AiModelStatus
import com.callassistant.ai.model.AiModelVariant
import com.callassistant.service.AiModelDownloadWorker
import com.callassistant.util.AiModelDownloadNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

interface AiModelRepository {
    val modelStatus: StateFlow<AiModelStatus>
    fun refreshStatus()
    suspend fun downloadModel(onProgress: (Float, Long, Long) -> Unit): Result<Unit>
    suspend fun deleteModel(): Result<Unit>
    suspend fun deleteModel(variant: AiModelVariant): Result<Unit>
    fun getModelFile(): File
    fun getModelFile(variant: AiModelVariant): File
    fun isDeviceSupported(): Boolean
    fun isVariantSupported(variant: AiModelVariant): Boolean
    fun getDeviceRamGb(): Double
    fun scheduleSilentDownloadIfNeeded()
    fun cancelDownload()
    fun startDownload()
    fun selectedDefinition(): AiModelDefinition
}

@Singleton
class AiModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : AiModelRepository {

    private val client = OkHttpClient.Builder().build()
    private val modelsDir = File(context.filesDir, "models").also { it.mkdirs() }

    private val _modelStatus = MutableStateFlow<AiModelStatus>(AiModelStatus.NotDownloaded)
    override val modelStatus: StateFlow<AiModelStatus> = _modelStatus.asStateFlow()

    @Volatile
    private var downloadCancelled = false

    init {
        refreshStatus()
    }

    override fun selectedDefinition(): AiModelDefinition =
        AiModelCatalog.get(settingsRepository.getSelectedAiModel())

    override fun refreshStatus() {
        val variant = settingsRepository.getSelectedAiModel()
        if (variant == AiModelVariant.AZURE_OPENAI) {
            _modelStatus.value = AiModelStatus.NotDownloaded
            return
        }
        val definition = AiModelCatalog.get(variant)
        if (!isArm64()) {
            _modelStatus.value = AiModelStatus.UnsupportedDevice
            return
        }
        if (!isVariantSupported(variant)) {
            _modelStatus.value = AiModelStatus.UnsupportedDevice
            return
        }
        val file = getModelFile(variant)
        val part = partFile(definition)
        if (file.exists() && isValidGgufFile(file) && file.length() >= minReadyBytes(definition)) {
            _modelStatus.value = AiModelStatus.Ready(file.absolutePath, file.length(), variant)
        } else {
            if (file.exists() && !isValidGgufFile(file)) {
                file.delete()
                part.delete()
            }
            _modelStatus.value = AiModelStatus.NotDownloaded
        }
    }

    override suspend fun downloadModel(onProgress: (Float, Long, Long) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            val definition = selectedDefinition()
            val variant = definition.variant
            if (variant == AiModelVariant.AZURE_OPENAI) {
                return@withContext Result.failure(
                    IllegalStateException("Azure OpenAI does not require a local model download")
                )
            }
            if (!isVariantSupported(variant)) {
                return@withContext Result.failure(
                    IllegalStateException("Device does not meet ${definition.displayName} requirements")
                )
            }
            downloadCancelled = false
            val target = getModelFile(variant)
            val temp = partFile(definition)
            modelsDir.mkdirs()
            var downloaded = if (temp.exists()) temp.length() else 0L

            AiModelDownloadNotifier.showProgress(
                context = context,
                modelName = definition.displayName,
                progressPercent = 0,
                bytesDone = downloaded,
                totalBytes = definition.sizeBytes
            )

            try {
                val requestBuilder = Request.Builder().url(definition.downloadUrl)
                if (downloaded > 0) {
                    requestBuilder.addHeader("Range", "bytes=$downloaded-")
                }
                val response = client.newCall(requestBuilder.build()).execute()
                when (response.code) {
                    206 -> Unit
                    200 -> {
                        if (downloaded > 0) {
                            downloaded = 0L
                            temp.delete()
                        }
                    }
                    416 -> {
                        if (downloaded == 0L) {
                            return@withContext Result.failure(
                                IllegalStateException("Download could not start. Check your connection and try again.")
                            )
                        }
                        downloaded = 0L
                        temp.delete()
                        return@withContext downloadModel(onProgress)
                    }
                    else -> {
                        if (!response.isSuccessful) {
                            return@withContext Result.failure(
                                IllegalStateException("Download failed: HTTP ${response.code}")
                            )
                        }
                    }
                }

                val body = response.body ?: return@withContext Result.failure(
                    IllegalStateException("Empty response body")
                )
                val contentLength = body.contentLength().coerceAtLeast(0L)
                val totalBytes = when (response.code) {
                    206 -> (downloaded + contentLength).coerceAtLeast(definition.sizeBytes)
                    else -> contentLength.coerceAtLeast(definition.sizeBytes)
                }

                val append = response.code == 206 && downloaded > 0 && temp.exists()
                body.byteStream().use { input ->
                    FileOutputStream(temp, append).use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            if (downloadCancelled) {
                                return@withContext Result.failure(CancelledDownload())
                            }
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val progress = (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            onProgress(progress, downloaded, totalBytes)
                            _modelStatus.value = AiModelStatus.Downloading(progress, downloaded, totalBytes)
                            AiModelDownloadNotifier.showProgress(
                                context = context,
                                modelName = definition.displayName,
                                progressPercent = (progress * 100).toInt(),
                                bytesDone = downloaded,
                                totalBytes = totalBytes
                            )
                        }
                        output.flush()
                    }
                }

                if (!temp.exists() || temp.length() <= 0L) {
                    return@withContext Result.failure(
                        IllegalStateException("Download incomplete — no data received")
                    )
                }

                if (!isValidGgufFile(temp)) {
                    temp.delete()
                    return@withContext Result.failure(
                        IllegalStateException("Downloaded file is not a valid model. Check Wi‑Fi and try again.")
                    )
                }

                if (temp.length() < minReadyBytes(definition)) {
                    temp.delete()
                    return@withContext Result.failure(
                        IllegalStateException("Download incomplete. Tap Retry download.")
                    )
                }

                if (definition.expectedSha256.isNotBlank()) {
                    val hash = sha256(temp)
                    if (!hash.equals(definition.expectedSha256, ignoreCase = true)) {
                        temp.delete()
                        return@withContext Result.failure(IllegalStateException("Checksum mismatch"))
                    }
                }

                if (target.exists()) target.delete()
                temp.copyTo(target, overwrite = true)
                temp.delete()

                settingsRepository.setAiModelDownloadedAt(System.currentTimeMillis())
                refreshStatus()
                AiModelDownloadNotifier.showComplete(context, definition.displayName)
                Result.success(Unit)
            } catch (e: Exception) {
                if (e is CancelledDownload) {
                    _modelStatus.value = AiModelStatus.NotDownloaded
                    AiModelDownloadNotifier.showCancelled(context, definition.displayName)
                } else {
                    _modelStatus.value = AiModelStatus.Error(toUserMessage(e))
                    AiModelDownloadNotifier.showFailed(
                        context,
                        definition.displayName,
                        toUserMessage(e)
                    )
                }
                Result.failure(e)
            }
        }

    override suspend fun deleteModel(): Result<Unit> =
        deleteModel(settingsRepository.getSelectedAiModel())

    override suspend fun deleteModel(variant: AiModelVariant): Result<Unit> = withContext(Dispatchers.IO) {
        val definition = AiModelCatalog.get(variant)
        getModelFile(variant).delete()
        partFile(definition).delete()
        if (variant == settingsRepository.getSelectedAiModel()) {
            settingsRepository.setAiModelDownloadedAt(0L)
            refreshStatus()
        }
        Result.success(Unit)
    }

    override fun getModelFile(): File = getModelFile(settingsRepository.getSelectedAiModel())

    override fun getModelFile(variant: AiModelVariant): File {
        require(variant.isOnDevice) { "Cloud models do not have a local file" }
        return File(modelsDir, AiModelCatalog.get(variant).fileName)
    }

    override fun isDeviceSupported(): Boolean =
        isArm64() && isVariantSupported(settingsRepository.getSelectedAiModel())

    override fun isVariantSupported(variant: AiModelVariant): Boolean {
        if (variant == AiModelVariant.AZURE_OPENAI) return true
        if (!isArm64()) return false
        return getDeviceRamGb() >= AiModelCatalog.get(variant).minRamGb
    }

    override fun getDeviceRamGb(): Double {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
    }

    override fun scheduleSilentDownloadIfNeeded() {
        if (!settingsRepository.isAiAssistantEnabled()) return
        if (!settingsRepository.isAiDownloadOnWifiOnly()) return
        if (settingsRepository.getSelectedAiModel() == AiModelVariant.AZURE_OPENAI) return
        val status = _modelStatus.value
        if (status is AiModelStatus.Ready || status is AiModelStatus.Downloading) return
        if (!isDeviceSupported()) return
        AiModelDownloadWorker.enqueue(context)
    }

    override fun cancelDownload() {
        downloadCancelled = true
        AiModelDownloadNotifier.dismiss(context)
        refreshStatus()
    }

    override fun startDownload() {
        AiModelDownloadWorker.enqueue(context, replaceExisting = true)
    }

    private fun partFile(definition: AiModelDefinition): File =
        File(modelsDir, "${definition.fileName}.part")

    private fun minReadyBytes(definition: AiModelDefinition): Long =
        (definition.sizeBytes * 0.85).toLong()

    private fun isValidGgufFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return file.inputStream().use { input ->
            val magic = ByteArray(4)
            input.read(magic) == 4 &&
                magic[0] == 'G'.code.toByte() &&
                magic[1] == 'G'.code.toByte() &&
                magic[2] == 'U'.code.toByte() &&
                magic[3] == 'F'.code.toByte()
        }
    }

    private fun toUserMessage(error: Exception): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("doesn't exist", ignoreCase = true) ->
                "Download interrupted. Tap Retry download."
            raw.contains("not a valid", ignoreCase = true) ->
                "Download failed or file is corrupt. Tap Retry download."
            raw.contains("Download incomplete", ignoreCase = true) ->
                "Download incomplete. Tap Retry download."
            raw.startsWith("Download failed: HTTP") ->
                "Could not download model. Connect to Wi‑Fi and try again."
            raw.contains("Unable to resolve host", ignoreCase = true) ->
                "No internet connection. Connect to Wi‑Fi and try again."
            raw.isNotBlank() -> raw
            else -> "Download failed. Please try again."
        }
    }

    private fun isArm64(): Boolean = Build.SUPPORTED_ABIS.firstOrNull() == "arm64-v8a"

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    class CancelledDownload : Exception("Download cancelled")
}
