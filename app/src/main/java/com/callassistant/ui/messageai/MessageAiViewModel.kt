package com.callassistant.ui.messageai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.ai.config.AzureOpenAiConfig
import com.callassistant.ai.engine.AzureOpenAiInferenceEngine
import com.callassistant.ai.engine.LlmInferenceEngine
import com.callassistant.ai.engine.NativeLlamaInferenceEngine
import com.callassistant.ai.engine.StubLlmInferenceEngine
import com.callassistant.ai.model.AiContextScope
import com.callassistant.ai.model.AiModelStatus
import com.callassistant.ai.model.AiModelVariant
import com.callassistant.ai.model.AiModelCatalog
import com.callassistant.ai.model.AiSuggestion
import com.callassistant.ai.model.AiTone
import com.callassistant.ai.model.MessageAiContext
import com.callassistant.ai.model.SuggestionLanguage
import com.callassistant.ai.prompt.MessagePromptBuilder
import com.callassistant.data.entity.SmsMessage
import com.callassistant.data.repository.AiModelRepository
import com.callassistant.data.repository.SettingsRepository
import com.callassistant.util.CLOUD_AI_INTERNET_REQUIRED_MESSAGE
import com.callassistant.util.NetworkConnectivityMonitor
import com.callassistant.util.cloudAiNetworkErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessageAiSheetState(
    val visible: Boolean = false,
    val contextScope: AiContextScope = AiContextScope.DEFAULT,
    val anchorMessage: SmsMessage? = null,
    val suggestions: List<AiSuggestion> = emptyList(),
    val selectedSuggestion: AiSuggestion? = null,
    val isGenerating: Boolean = false,
    val isRefiningTone: Boolean = false,
    val askQuery: String = "",
    val error: String? = null,
    val usingFullModel: Boolean = false
)

data class MessageAiUiState(
    val sheet: MessageAiSheetState = MessageAiSheetState(),
    val modelStatus: AiModelStatus = AiModelStatus.NotDownloaded,
    val selectedModel: AiModelVariant = AiModelVariant.DEFAULT,
    val deviceRamGb: Double = 0.0,
    val aiEnabled: Boolean = true,
    val nativeRuntimeAvailable: Boolean = false,
    val azureConfigured: Boolean = false,
    val azureModelName: String = "",
    val inferenceReady: Boolean = false,
    val pendingInsertText: String? = null
)

@HiltViewModel
class MessageAiViewModel @Inject constructor(
    private val aiModelRepository: AiModelRepository,
    private val settingsRepository: SettingsRepository,
    private val inferenceEngine: LlmInferenceEngine,
    private val nativeEngine: NativeLlamaInferenceEngine,
    private val azureEngine: AzureOpenAiInferenceEngine,
    private val azureConfig: AzureOpenAiConfig,
    private val stubEngine: StubLlmInferenceEngine,
    private val promptBuilder: MessagePromptBuilder,
    private val networkMonitor: NetworkConnectivityMonitor
) : ViewModel() {

    private val _sheet = MutableStateFlow(MessageAiSheetState())
    private var activeContext: MessageAiContext? = null
    private var generationJob: Job? = null

    val uiState: StateFlow<MessageAiUiState> = combine(
        _sheet,
        aiModelRepository.modelStatus,
        settingsRepository.selectedAiModel
    ) { sheet, modelStatus, selectedModel ->
        MessageAiUiState(
            sheet = sheet,
            modelStatus = modelStatus,
            selectedModel = selectedModel,
            deviceRamGb = aiModelRepository.getDeviceRamGb(),
            aiEnabled = settingsRepository.isAiAssistantEnabled(),
            nativeRuntimeAvailable = nativeEngine.isNativeAvailable,
            azureConfigured = azureEngine.isLoaded,
            azureModelName = azureConfig.deployment,
            inferenceReady = isInferenceReady(selectedModel, modelStatus),
            pendingInsertText = null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessageAiUiState())

    private val _insertEvent = MutableStateFlow<String?>(null)
    val insertEvent: StateFlow<String?> = _insertEvent.asStateFlow()

    init {
        aiModelRepository.refreshStatus()
    }

    fun openSheet(
        scope: AiContextScope = settingsRepository.getAiDefaultContextScope(),
        anchorMessage: SmsMessage? = null
    ) {
        _sheet.update {
            MessageAiSheetState(
                visible = true,
                contextScope = if (anchorMessage != null) AiContextScope.THIS_MESSAGE else scope,
                anchorMessage = anchorMessage
            )
        }
    }

    fun dismissSheet() {
        generationJob?.cancel()
        inferenceEngine.cancel()
        stubEngine.cancel()
        _sheet.update { MessageAiSheetState() }
        activeContext = null
    }

    fun setContextScope(scope: AiContextScope) {
        _sheet.update {
            it.copy(
                contextScope = scope,
                suggestions = emptyList(),
                selectedSuggestion = null,
                isGenerating = false
            )
        }
        settingsRepository.setAiDefaultContextScope(scope)
    }

    fun selectModel(variant: AiModelVariant) {
        if (settingsRepository.getSelectedAiModel() == variant) return
        generationJob?.cancel()
        inferenceEngine.unload()
        settingsRepository.setSelectedAiModel(variant)
        aiModelRepository.refreshStatus()
        _sheet.update {
            it.copy(suggestions = emptyList(), selectedSuggestion = null, error = null)
        }
    }

    fun isModelSupported(variant: AiModelVariant): Boolean = when (variant) {
        AiModelVariant.AZURE_OPENAI -> true
        else -> aiModelRepository.isVariantSupported(variant)
    }

    fun isModelDownloaded(variant: AiModelVariant): Boolean = when (variant) {
        AiModelVariant.AZURE_OPENAI -> azureEngine.isLoaded
        else -> {
            val file = aiModelRepository.getModelFile(variant)
            val def = AiModelCatalog.get(variant)
            file.exists() && file.length() > def.sizeBytes / 10
        }
    }

    fun setAskQuery(query: String) {
        _sheet.update { it.copy(askQuery = query) }
    }

    fun generateSuggestions(context: MessageAiContext) {
        activeContext = context.copy(scope = _sheet.value.contextScope, anchorMessage = _sheet.value.anchorMessage)
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _sheet.update {
                it.copy(isGenerating = true, error = null, suggestions = emptyList(), selectedSuggestion = null)
            }
            try {
                ensureModelLoadedIfReady()
                val ctx = activeContext ?: return@launch
                val custom = _sheet.value.askQuery.trim().takeIf { it.isNotBlank() }
                val useAzure = shouldUseAzureEngine()
                val useNative = !useAzure && shouldUseNativeEngine()
                if (useAzure) {
                    ensureCloudAiNetworkAvailable()
                }
                val pairs = when {
                    useAzure -> generateWithAzure(ctx, custom)
                    !useNative -> stubEngine.generateSuggestions(ctx, custom)
                    else -> {
                        try {
                            val prompt = promptBuilder.buildSuggestionPrompt(ctx, custom)
                            val result = inferenceEngine.complete(prompt, maxTokens = if (custom != null) 80 else 120)
                            val raw = result.getOrThrow()
                            val parsed = promptBuilder.parseSuggestions(raw)
                            when {
                                parsed.isNotEmpty() -> parsed
                                custom != null -> listOf(
                                    SuggestionLanguage.EN to promptBuilder.parseSingleReply(raw)
                                )
                                else -> stubEngine.generateSuggestions(ctx, custom)
                            }
                        } catch (e: Exception) {
                            stubEngine.generateSuggestions(ctx, custom)
                        }
                    }
                }
                val suggestions = pairs.map { (lang, text) -> AiSuggestion(text, lang) }
                _sheet.update {
                    it.copy(
                        isGenerating = false,
                        suggestions = suggestions,
                        usingFullModel = useNative || useAzure
                    )
                }
            } catch (e: Exception) {
                _sheet.update {
                    it.copy(isGenerating = false, error = e.message ?: "Could not generate suggestions")
                }
            }
        }
    }

    fun selectSuggestion(suggestion: AiSuggestion) {
        _insertEvent.value = suggestion.text
        dismissSheet()
    }

    fun consumeInsertEvent() {
        _insertEvent.value = null
    }

    fun refineTone(tone: AiTone) {
        val selected = _sheet.value.selectedSuggestion ?: return
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _sheet.update { it.copy(isRefiningTone = true, error = null) }
            try {
                val useAzure = shouldUseAzureEngine()
                val useNative = !useAzure && shouldUseNativeEngine()
                if (useAzure) {
                    ensureCloudAiNetworkAvailable()
                }
                val refined = when {
                    useAzure -> refineWithAzure(selected, tone)
                    !useNative -> stubEngine.refineTone(selected.text, tone, selected.language)
                    else -> {
                        try {
                            val prompt = promptBuilder.buildTonePrompt(selected.text, tone, selected.language)
                            inferenceEngine.complete(prompt, maxTokens = 80).getOrThrow().let {
                                promptBuilder.parseSingleReply(it)
                            }
                        } catch (e: Exception) {
                            stubEngine.refineTone(selected.text, tone, selected.language)
                        }
                    }
                }
                val updated = selected.copy(text = refined)
                _sheet.update {
                    it.copy(
                        isRefiningTone = false,
                        selectedSuggestion = updated,
                        suggestions = it.suggestions.map { s ->
                            if (s.language == updated.language && s.text == selected.text) updated else s
                        }
                    )
                }
                _insertEvent.value = refined
            } catch (e: Exception) {
                _sheet.update {
                    it.copy(isRefiningTone = false, error = e.message ?: "Tone refine failed")
                }
            }
        }
    }

    fun downloadModel() {
        aiModelRepository.startDownload()
    }

    fun deleteModel() {
        viewModelScope.launch {
            inferenceEngine.unload()
            aiModelRepository.deleteModel()
        }
    }

    fun cancelDownload() {
        aiModelRepository.cancelDownload()
        aiModelRepository.refreshStatus()
    }

    fun scheduleSilentDownload() {
        aiModelRepository.scheduleSilentDownloadIfNeeded()
    }

    private suspend fun ensureModelLoadedIfReady() {
        val status = aiModelRepository.modelStatus.value
        if (status is AiModelStatus.Ready && !inferenceEngine.isLoaded) {
            inferenceEngine.load(status.path)
        }
    }

    private suspend fun shouldUseAzureEngine(): Boolean =
        settingsRepository.getSelectedAiModel() == AiModelVariant.AZURE_OPENAI && azureEngine.isLoaded

    private suspend fun shouldUseNativeEngine(): Boolean {
        if (settingsRepository.getSelectedAiModel() == AiModelVariant.AZURE_OPENAI) return false
        if (!nativeEngine.isNativeAvailable) return false
        val status = aiModelRepository.modelStatus.value
        if (status !is AiModelStatus.Ready) return false
        if (!inferenceEngine.isLoaded) {
            val loaded = inferenceEngine.load(status.path)
            if (loaded.isFailure) return false
        }
        return true
    }

    private fun isInferenceReady(
        selectedModel: AiModelVariant,
        modelStatus: AiModelStatus
    ): Boolean = when (selectedModel) {
        AiModelVariant.AZURE_OPENAI -> azureEngine.isLoaded
        else -> modelStatus is AiModelStatus.Ready
    }

    private fun ensureCloudAiNetworkAvailable() {
        if (!networkMonitor.isOnline()) {
            throw IllegalStateException(CLOUD_AI_INTERNET_REQUIRED_MESSAGE)
        }
    }

    private suspend fun generateWithAzure(
        ctx: MessageAiContext,
        custom: String?
    ): List<Pair<SuggestionLanguage, String>> {
        val messages = promptBuilder.buildSuggestionMessages(ctx, custom)
        val result = azureEngine.completeChat(
            messages,
            maxTokens = if (custom != null) 80 else 120
        )
        val raw = result.getOrElse { error ->
            throw IllegalStateException(cloudAiNetworkErrorMessage(error), error)
        }
        val parsed = promptBuilder.parseSuggestions(raw)
        return when {
            parsed.isNotEmpty() -> parsed
            custom != null -> listOf(SuggestionLanguage.EN to promptBuilder.parseSingleReply(raw))
            else -> throw IllegalStateException("Cloud AI returned an empty response. Try again.")
        }
    }

    private suspend fun refineWithAzure(
        selected: AiSuggestion,
        tone: AiTone
    ): String {
        val messages = promptBuilder.buildToneMessages(selected.text, tone, selected.language)
        val result = azureEngine.completeChat(messages, maxTokens = 80)
        return result.getOrElse { error ->
            throw IllegalStateException(cloudAiNetworkErrorMessage(error), error)
        }.let { promptBuilder.parseSingleReply(it) }
    }
}
