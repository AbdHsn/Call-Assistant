package com.callassistant.ui.messageai;

import com.callassistant.ai.config.AzureOpenAiConfig;
import com.callassistant.ai.engine.AzureOpenAiInferenceEngine;
import com.callassistant.ai.engine.LlmInferenceEngine;
import com.callassistant.ai.engine.NativeLlamaInferenceEngine;
import com.callassistant.ai.engine.StubLlmInferenceEngine;
import com.callassistant.ai.prompt.MessagePromptBuilder;
import com.callassistant.data.repository.AiModelRepository;
import com.callassistant.data.repository.SettingsRepository;
import com.callassistant.util.NetworkConnectivityMonitor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class MessageAiViewModel_Factory implements Factory<MessageAiViewModel> {
  private final Provider<AiModelRepository> aiModelRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<LlmInferenceEngine> inferenceEngineProvider;

  private final Provider<NativeLlamaInferenceEngine> nativeEngineProvider;

  private final Provider<AzureOpenAiInferenceEngine> azureEngineProvider;

  private final Provider<AzureOpenAiConfig> azureConfigProvider;

  private final Provider<StubLlmInferenceEngine> stubEngineProvider;

  private final Provider<MessagePromptBuilder> promptBuilderProvider;

  private final Provider<NetworkConnectivityMonitor> networkMonitorProvider;

  public MessageAiViewModel_Factory(Provider<AiModelRepository> aiModelRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<LlmInferenceEngine> inferenceEngineProvider,
      Provider<NativeLlamaInferenceEngine> nativeEngineProvider,
      Provider<AzureOpenAiInferenceEngine> azureEngineProvider,
      Provider<AzureOpenAiConfig> azureConfigProvider,
      Provider<StubLlmInferenceEngine> stubEngineProvider,
      Provider<MessagePromptBuilder> promptBuilderProvider,
      Provider<NetworkConnectivityMonitor> networkMonitorProvider) {
    this.aiModelRepositoryProvider = aiModelRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.inferenceEngineProvider = inferenceEngineProvider;
    this.nativeEngineProvider = nativeEngineProvider;
    this.azureEngineProvider = azureEngineProvider;
    this.azureConfigProvider = azureConfigProvider;
    this.stubEngineProvider = stubEngineProvider;
    this.promptBuilderProvider = promptBuilderProvider;
    this.networkMonitorProvider = networkMonitorProvider;
  }

  @Override
  public MessageAiViewModel get() {
    return newInstance(aiModelRepositoryProvider.get(), settingsRepositoryProvider.get(), inferenceEngineProvider.get(), nativeEngineProvider.get(), azureEngineProvider.get(), azureConfigProvider.get(), stubEngineProvider.get(), promptBuilderProvider.get(), networkMonitorProvider.get());
  }

  public static MessageAiViewModel_Factory create(
      Provider<AiModelRepository> aiModelRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<LlmInferenceEngine> inferenceEngineProvider,
      Provider<NativeLlamaInferenceEngine> nativeEngineProvider,
      Provider<AzureOpenAiInferenceEngine> azureEngineProvider,
      Provider<AzureOpenAiConfig> azureConfigProvider,
      Provider<StubLlmInferenceEngine> stubEngineProvider,
      Provider<MessagePromptBuilder> promptBuilderProvider,
      Provider<NetworkConnectivityMonitor> networkMonitorProvider) {
    return new MessageAiViewModel_Factory(aiModelRepositoryProvider, settingsRepositoryProvider, inferenceEngineProvider, nativeEngineProvider, azureEngineProvider, azureConfigProvider, stubEngineProvider, promptBuilderProvider, networkMonitorProvider);
  }

  public static MessageAiViewModel newInstance(AiModelRepository aiModelRepository,
      SettingsRepository settingsRepository, LlmInferenceEngine inferenceEngine,
      NativeLlamaInferenceEngine nativeEngine, AzureOpenAiInferenceEngine azureEngine,
      AzureOpenAiConfig azureConfig, StubLlmInferenceEngine stubEngine,
      MessagePromptBuilder promptBuilder, NetworkConnectivityMonitor networkMonitor) {
    return new MessageAiViewModel(aiModelRepository, settingsRepository, inferenceEngine, nativeEngine, azureEngine, azureConfig, stubEngine, promptBuilder, networkMonitor);
  }
}
