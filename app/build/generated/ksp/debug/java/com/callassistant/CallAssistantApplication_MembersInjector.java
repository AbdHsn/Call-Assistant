package com.callassistant;

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

  public CallAssistantApplication_MembersInjector(
      Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    this.spamRuleRepositoryProvider = spamRuleRepositoryProvider;
  }

  public static MembersInjector<CallAssistantApplication> create(
      Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    return new CallAssistantApplication_MembersInjector(spamRuleRepositoryProvider);
  }

  @Override
  public void injectMembers(CallAssistantApplication instance) {
    injectSpamRuleRepository(instance, spamRuleRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.callassistant.CallAssistantApplication.spamRuleRepository")
  public static void injectSpamRuleRepository(CallAssistantApplication instance,
      SpamRuleRepository spamRuleRepository) {
    instance.spamRuleRepository = spamRuleRepository;
  }
}
