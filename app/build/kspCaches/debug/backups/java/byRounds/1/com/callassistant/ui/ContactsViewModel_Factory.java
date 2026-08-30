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
public final class ContactsViewModel_Factory implements Factory<ContactsViewModel> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<CallLogRepository> callLogRepositoryProvider;

  private final Provider<SpamRuleRepository> spamRuleRepositoryProvider;

  public ContactsViewModel_Factory(Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.callLogRepositoryProvider = callLogRepositoryProvider;
    this.spamRuleRepositoryProvider = spamRuleRepositoryProvider;
  }

  @Override
  public ContactsViewModel get() {
    return newInstance(contactRepositoryProvider.get(), callLogRepositoryProvider.get(), spamRuleRepositoryProvider.get());
  }

  public static ContactsViewModel_Factory create(
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    return new ContactsViewModel_Factory(contactRepositoryProvider, callLogRepositoryProvider, spamRuleRepositoryProvider);
  }

  public static ContactsViewModel newInstance(ContactRepository contactRepository,
      CallLogRepository callLogRepository, SpamRuleRepository spamRuleRepository) {
    return new ContactsViewModel(contactRepository, callLogRepository, spamRuleRepository);
  }
}
