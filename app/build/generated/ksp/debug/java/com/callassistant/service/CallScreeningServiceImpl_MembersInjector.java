package com.callassistant.service;

import com.callassistant.data.repository.CallLogRepository;
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
public final class CallScreeningServiceImpl_MembersInjector implements MembersInjector<CallScreeningServiceImpl> {
  private final Provider<SpamRuleRepository> repoProvider;

  private final Provider<CallLogRepository> callLogRepositoryProvider;

  public CallScreeningServiceImpl_MembersInjector(Provider<SpamRuleRepository> repoProvider,
      Provider<CallLogRepository> callLogRepositoryProvider) {
    this.repoProvider = repoProvider;
    this.callLogRepositoryProvider = callLogRepositoryProvider;
  }

  public static MembersInjector<CallScreeningServiceImpl> create(
      Provider<SpamRuleRepository> repoProvider,
      Provider<CallLogRepository> callLogRepositoryProvider) {
    return new CallScreeningServiceImpl_MembersInjector(repoProvider, callLogRepositoryProvider);
  }

  @Override
  public void injectMembers(CallScreeningServiceImpl instance) {
    injectRepo(instance, repoProvider.get());
    injectCallLogRepository(instance, callLogRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.callassistant.service.CallScreeningServiceImpl.repo")
  public static void injectRepo(CallScreeningServiceImpl instance, SpamRuleRepository repo) {
    instance.repo = repo;
  }

  @InjectedFieldSignature("com.callassistant.service.CallScreeningServiceImpl.callLogRepository")
  public static void injectCallLogRepository(CallScreeningServiceImpl instance,
      CallLogRepository callLogRepository) {
    instance.callLogRepository = callLogRepository;
  }
}
