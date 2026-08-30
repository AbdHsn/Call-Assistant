package com.callassistant.ui;

import com.callassistant.data.repository.CallLogRepository;
import com.callassistant.data.repository.ContactRepository;
import com.callassistant.data.repository.SpamRuleRepository;
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
public final class CallLogViewModel_Factory implements Factory<CallLogViewModel> {
  private final Provider<CallLogRepository> callLogRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<SpamRuleRepository> spamRuleRepositoryProvider;

  public CallLogViewModel_Factory(Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    this.callLogRepositoryProvider = callLogRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.spamRuleRepositoryProvider = spamRuleRepositoryProvider;
  }

  @Override
  public CallLogViewModel get() {
    return newInstance(callLogRepositoryProvider.get(), contactRepositoryProvider.get(), spamRuleRepositoryProvider.get());
  }

  public static CallLogViewModel_Factory create(
      Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    return new CallLogViewModel_Factory(callLogRepositoryProvider, contactRepositoryProvider, spamRuleRepositoryProvider);
  }

  public static CallLogViewModel newInstance(CallLogRepository callLogRepository,
      ContactRepository contactRepository, SpamRuleRepository spamRuleRepository) {
    return new CallLogViewModel(callLogRepository, contactRepository, spamRuleRepository);
  }
}
