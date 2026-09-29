package com.callassistant;

import com.callassistant.data.repository.AiModelRepository;
import com.callassistant.data.repository.SpamRuleRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class CallAssistantApplication_MembersInjector implements MembersInjector<CallAssistantApplication> {
  private final Provider<SpamRuleRepository> spamRuleRepositoryProvider;

  private final Provider<AiModelRepository> aiModelRepositoryProvider;

  public CallAssistantApplication_MembersInjector(
      Provider<SpamRuleRepository> spamRuleRepositoryProvider,
      Provider<AiModelRepository> aiModelRepositoryProvider) {
    this.spamRuleRepositoryProvider = spamRuleRepositoryProvider;
    this.aiModelRepositoryProvider = aiModelRepositoryProvider;
  }

  public static MembersInjector<CallAssistantApplication> create(
      Provider<SpamRuleRepository> spamRuleRepositoryProvider,
      Provider<AiModelRepository> aiModelRepositoryProvider) {
    return new CallAssistantApplication_MembersInjector(spamRuleRepositoryProvider, aiModelRepositoryProvider);
  }

  @Override
  public void injectMembers(CallAssistantApplication instance) {
    injectSpamRuleRepository(instance, spamRuleRepositoryProvider.get());
    injectAiModelRepository(instance, aiModelRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.callassistant.CallAssistantApplication.spamRuleRepository")
  public static void injectSpamRuleRepository(CallAssistantApplication instance,
      SpamRuleRepository spamRuleRepository) {
    instance.spamRuleRepository = spamRuleRepository;
  }

  @InjectedFieldSignature("com.callassistant.CallAssistantApplication.aiModelRepository")
  public static void injectAiModelRepository(CallAssistantApplication instance,
      AiModelRepository aiModelRepository) {
    instance.aiModelRepository = aiModelRepository;
  }
}
