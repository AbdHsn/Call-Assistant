package com.callassistant.data.repository;

import android.content.Context;
import com.callassistant.data.db.AppDatabase;
import com.callassistant.data.sync.CallLogSyncer;
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
public final class CallLogRepositoryImpl_Factory implements Factory<CallLogRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<AppDatabase> dbProvider;

  private final Provider<CallLogSyncer> callLogSyncerProvider;

  public CallLogRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<AppDatabase> dbProvider, Provider<CallLogSyncer> callLogSyncerProvider) {
    this.contextProvider = contextProvider;
    this.dbProvider = dbProvider;
    this.callLogSyncerProvider = callLogSyncerProvider;
  }

  @Override
  public CallLogRepositoryImpl get() {
    return newInstance(contextProvider.get(), dbProvider.get(), callLogSyncerProvider.get());
  }

  public static CallLogRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<AppDatabase> dbProvider, Provider<CallLogSyncer> callLogSyncerProvider) {
    return new CallLogRepositoryImpl_Factory(contextProvider, dbProvider, callLogSyncerProvider);
  }

  public static CallLogRepositoryImpl newInstance(Context context, AppDatabase db,
      CallLogSyncer callLogSyncer) {
    return new CallLogRepositoryImpl(context, db, callLogSyncer);
  }
}
