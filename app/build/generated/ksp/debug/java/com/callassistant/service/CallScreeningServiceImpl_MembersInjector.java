package com.callassistant.service;

import com.callassistant.data.db.AppDatabase;
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

  private final Provider<AppDatabase> dbProvider;

  public CallScreeningServiceImpl_MembersInjector(Provider<SpamRuleRepository> repoProvider,
      Provider<AppDatabase> dbProvider) {
    this.repoProvider = repoProvider;
    this.dbProvider = dbProvider;
  }

  public static MembersInjector<CallScreeningServiceImpl> create(
      Provider<SpamRuleRepository> repoProvider, Provider<AppDatabase> dbProvider) {
    return new CallScreeningServiceImpl_MembersInjector(repoProvider, dbProvider);
  }

  @Override
  public void injectMembers(CallScreeningServiceImpl instance) {
    injectRepo(instance, repoProvider.get());
    injectDb(instance, dbProvider.get());
  }

  @InjectedFieldSignature("com.callassistant.service.CallScreeningServiceImpl.repo")
  public static void injectRepo(CallScreeningServiceImpl instance, SpamRuleRepository repo) {
    instance.repo = repo;
  }

  @InjectedFieldSignature("com.callassistant.service.CallScreeningServiceImpl.db")
  public static void injectDb(CallScreeningServiceImpl instance, AppDatabase db) {
    instance.db = db;
  }
}
