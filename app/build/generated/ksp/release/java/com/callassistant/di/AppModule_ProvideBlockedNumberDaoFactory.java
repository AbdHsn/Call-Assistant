package com.callassistant.di;

import com.callassistant.data.db.AppDatabase;
import com.callassistant.data.db.BlockedNumberDao;
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
public final class AppModule_ProvideBlockedNumberDaoFactory implements Factory<BlockedNumberDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideBlockedNumberDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BlockedNumberDao get() {
    return provideBlockedNumberDao(dbProvider.get());
  }

  public static AppModule_ProvideBlockedNumberDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideBlockedNumberDaoFactory(dbProvider);
  }

  public static BlockedNumberDao provideBlockedNumberDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBlockedNumberDao(db));
  }
}
