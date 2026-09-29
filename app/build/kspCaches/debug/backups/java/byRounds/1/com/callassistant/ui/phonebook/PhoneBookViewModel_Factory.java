package com.callassistant.ui.phonebook;

import android.content.Context;
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
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class PhoneBookViewModel_Factory implements Factory<PhoneBookViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<CallLogRepository> callLogRepositoryProvider;

  private final Provider<SpamRuleRepository> spamRuleRepositoryProvider;

  public PhoneBookViewModel_Factory(Provider<Context> contextProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.callLogRepositoryProvider = callLogRepositoryProvider;
    this.spamRuleRepositoryProvider = spamRuleRepositoryProvider;
  }

  @Override
  public PhoneBookViewModel get() {
    return newInstance(contextProvider.get(), contactRepositoryProvider.get(), callLogRepositoryProvider.get(), spamRuleRepositoryProvider.get());
  }

  public static PhoneBookViewModel_Factory create(Provider<Context> contextProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    return new PhoneBookViewModel_Factory(contextProvider, contactRepositoryProvider, callLogRepositoryProvider, spamRuleRepositoryProvider);
  }

  public static PhoneBookViewModel newInstance(Context context, ContactRepository contactRepository,
      CallLogRepository callLogRepository, SpamRuleRepository spamRuleRepository) {
    return new PhoneBookViewModel(context, contactRepository, callLogRepository, spamRuleRepository);
  }
}
