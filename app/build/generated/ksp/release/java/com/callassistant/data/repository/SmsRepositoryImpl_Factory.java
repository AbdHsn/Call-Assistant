package com.callassistant.data.repository;

import android.content.Context;
import com.callassistant.data.db.AppDatabase;
import com.callassistant.data.sync.SmsSyncer;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class SmsRepositoryImpl_Factory implements Factory<SmsRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<AppDatabase> dbProvider;

  private final Provider<SmsSyncer> smsSyncerProvider;

  public SmsRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<AppDatabase> dbProvider, Provider<SmsSyncer> smsSyncerProvider) {
    this.contextProvider = contextProvider;
    this.dbProvider = dbProvider;
    this.smsSyncerProvider = smsSyncerProvider;
  }

  @Override
  public SmsRepositoryImpl get() {
    return newInstance(contextProvider.get(), dbProvider.get(), smsSyncerProvider.get());
  }

  public static SmsRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<AppDatabase> dbProvider, Provider<SmsSyncer> smsSyncerProvider) {
    return new SmsRepositoryImpl_Factory(contextProvider, dbProvider, smsSyncerProvider);
  }

  public static SmsRepositoryImpl newInstance(Context context, AppDatabase db,
      SmsSyncer smsSyncer) {
    return new SmsRepositoryImpl(context, db, smsSyncer);
  }
}
