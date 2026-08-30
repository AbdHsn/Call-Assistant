package com.callassistant.ui;

import com.callassistant.data.repository.CallLogRepository;
import com.callassistant.data.repository.ContactRepository;
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
public final class DialPadViewModel_Factory implements Factory<DialPadViewModel> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<CallLogRepository> callLogRepositoryProvider;

  public DialPadViewModel_Factory(Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.callLogRepositoryProvider = callLogRepositoryProvider;
  }

  @Override
  public DialPadViewModel get() {
    return newInstance(contactRepositoryProvider.get(), callLogRepositoryProvider.get());
  }

  public static DialPadViewModel_Factory create(
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider) {
    return new DialPadViewModel_Factory(contactRepositoryProvider, callLogRepositoryProvider);
  }

  public static DialPadViewModel newInstance(ContactRepository contactRepository,
      CallLogRepository callLogRepository) {
    return new DialPadViewModel(contactRepository, callLogRepository);
  }
}
