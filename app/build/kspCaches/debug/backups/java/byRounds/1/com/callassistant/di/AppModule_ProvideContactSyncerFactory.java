package com.callassistant.di;

import android.content.Context;
import com.callassistant.data.sync.ContactSyncer;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AppModule_ProvideContactSyncerFactory implements Factory<ContactSyncer> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideContactSyncerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ContactSyncer get() {
    return provideContactSyncer(contextProvider.get());
  }

  public static AppModule_ProvideContactSyncerFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideContactSyncerFactory(contextProvider);
  }

  public static ContactSyncer provideContactSyncer(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideContactSyncer(context));
  }
}
