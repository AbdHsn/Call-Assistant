package com.callassistant.di;

import com.callassistant.data.db.AppDatabase;
import com.callassistant.data.repository.SpamRuleRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideSpamRuleRepositoryFactory implements Factory<SpamRuleRepository> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideSpamRuleRepositoryFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SpamRuleRepository get() {
    return provideSpamRuleRepository(dbProvider.get());
  }

  public static AppModule_ProvideSpamRuleRepositoryFactory create(
      Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideSpamRuleRepositoryFactory(dbProvider);
  }

  public static SpamRuleRepository provideSpamRuleRepository(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSpamRuleRepository(db));
  }
}
