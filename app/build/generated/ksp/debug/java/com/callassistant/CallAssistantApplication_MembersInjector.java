package com.callassistant;

import com.callassistant.data.db.AppDatabase;
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
  private final Provider<AppDatabase> databaseProvider;

  public CallAssistantApplication_MembersInjector(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  public static MembersInjector<CallAssistantApplication> create(
      Provider<AppDatabase> databaseProvider) {
    return new CallAssistantApplication_MembersInjector(databaseProvider);
  }

  @Override
  public void injectMembers(CallAssistantApplication instance) {
    injectDatabase(instance, databaseProvider.get());
  }

  @InjectedFieldSignature("com.callassistant.CallAssistantApplication.database")
  public static void injectDatabase(CallAssistantApplication instance, AppDatabase database) {
    instance.database = database;
  }
}
