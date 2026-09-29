package com.callassistant.di;

import com.callassistant.data.db.AppDatabase;
import com.callassistant.data.db.SpamRuleDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideSpamRuleDaoFactory implements Factory<SpamRuleDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideSpamRuleDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SpamRuleDao get() {
    return provideSpamRuleDao(dbProvider.get());
  }

  public static AppModule_ProvideSpamRuleDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideSpamRuleDaoFactory(dbProvider);
  }

  public static SpamRuleDao provideSpamRuleDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSpamRuleDao(db));
  }
}
