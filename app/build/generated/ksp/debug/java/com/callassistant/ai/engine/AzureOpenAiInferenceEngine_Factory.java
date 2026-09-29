package com.callassistant.ai.engine;

import com.callassistant.ai.config.AzureOpenAiConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AzureOpenAiInferenceEngine_Factory implements Factory<AzureOpenAiInferenceEngine> {
  private final Provider<AzureOpenAiConfig> configProvider;

  public AzureOpenAiInferenceEngine_Factory(Provider<AzureOpenAiConfig> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public AzureOpenAiInferenceEngine get() {
    return newInstance(configProvider.get());
  }

  public static AzureOpenAiInferenceEngine_Factory create(
      Provider<AzureOpenAiConfig> configProvider) {
    return new AzureOpenAiInferenceEngine_Factory(configProvider);
  }

  public static AzureOpenAiInferenceEngine newInstance(AzureOpenAiConfig config) {
    return new AzureOpenAiInferenceEngine(config);
  }
}
