package com.callassistant.ui;

import com.callassistant.data.repository.ContactRepository;
import com.callassistant.data.repository.SmsRepository;
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
public final class MessagesViewModel_Factory implements Factory<MessagesViewModel> {
  private final Provider<SmsRepository> smsRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public MessagesViewModel_Factory(Provider<SmsRepository> smsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.smsRepositoryProvider = smsRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public MessagesViewModel get() {
    return newInstance(smsRepositoryProvider.get(), contactRepositoryProvider.get());
  }

  public static MessagesViewModel_Factory create(Provider<SmsRepository> smsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new MessagesViewModel_Factory(smsRepositoryProvider, contactRepositoryProvider);
  }

  public static MessagesViewModel newInstance(SmsRepository smsRepository,
      ContactRepository contactRepository) {
    return new MessagesViewModel(smsRepository, contactRepository);
  }
}
