package com.callassistant.data.repository;

import android.content.Context;
import com.callassistant.data.db.AppDatabase;
import com.callassistant.data.sync.ContactSyncer;
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
public final class ContactRepositoryImpl_Factory implements Factory<ContactRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<AppDatabase> dbProvider;

  private final Provider<ContactSyncer> contactSyncerProvider;

  public ContactRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<AppDatabase> dbProvider, Provider<ContactSyncer> contactSyncerProvider) {
    this.contextProvider = contextProvider;
    this.dbProvider = dbProvider;
    this.contactSyncerProvider = contactSyncerProvider;
  }

  @Override
  public ContactRepositoryImpl get() {
    return newInstance(contextProvider.get(), dbProvider.get(), contactSyncerProvider.get());
  }

  public static ContactRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<AppDatabase> dbProvider, Provider<ContactSyncer> contactSyncerProvider) {
    return new ContactRepositoryImpl_Factory(contextProvider, dbProvider, contactSyncerProvider);
  }

  public static ContactRepositoryImpl newInstance(Context context, AppDatabase db,
      ContactSyncer contactSyncer) {
    return new ContactRepositoryImpl(context, db, contactSyncer);
  }
}
