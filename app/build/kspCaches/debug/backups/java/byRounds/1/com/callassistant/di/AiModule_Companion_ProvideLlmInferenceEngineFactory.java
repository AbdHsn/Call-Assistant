package com.callassistant.di;

import com.callassistant.ai.engine.LlmInferenceEngine;
import com.callassistant.ai.engine.NativeLlamaInferenceEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AiModule_Companion_ProvideLlmInferenceEngineFactory implements Factory<LlmInferenceEngine> {
  private final Provider<NativeLlamaInferenceEngine> nativeProvider;

  public AiModule_Companion_ProvideLlmInferenceEngineFactory(
      Provider<NativeLlamaInferenceEngine> nativeProvider) {
    this.nativeProvider = nativeProvider;
  }

  @Override
  public LlmInferenceEngine get() {
    return provideLlmInferenceEngine(nativeProvider.get());
  }

  public static AiModule_Companion_ProvideLlmInferenceEngineFactory create(
      Provider<NativeLlamaInferenceEngine> nativeProvider) {
    return new AiModule_Companion_ProvideLlmInferenceEngineFactory(nativeProvider);
  }

  public static LlmInferenceEngine provideLlmInferenceEngine(NativeLlamaInferenceEngine p0) {
    return Preconditions.checkNotNullFromProvides(AiModule.Companion.provideLlmInferenceEngine(p0));
  }
}
