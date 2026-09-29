package com.callassistant.ai.engine;

import com.callassistant.ai.prompt.MessagePromptBuilder;
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
public final class StubLlmInferenceEngine_Factory implements Factory<StubLlmInferenceEngine> {
  private final Provider<MessagePromptBuilder> promptBuilderProvider;

  public StubLlmInferenceEngine_Factory(Provider<MessagePromptBuilder> promptBuilderProvider) {
    this.promptBuilderProvider = promptBuilderProvider;
  }

  @Override
  public StubLlmInferenceEngine get() {
    return newInstance(promptBuilderProvider.get());
  }

  public static StubLlmInferenceEngine_Factory create(
      Provider<MessagePromptBuilder> promptBuilderProvider) {
    return new StubLlmInferenceEngine_Factory(promptBuilderProvider);
  }

  public static StubLlmInferenceEngine newInstance(MessagePromptBuilder promptBuilder) {
    return new StubLlmInferenceEngine(promptBuilder);
  }
}
