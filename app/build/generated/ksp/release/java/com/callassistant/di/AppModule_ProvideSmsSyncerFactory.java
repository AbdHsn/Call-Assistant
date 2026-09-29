package com.callassistant.di;

import android.content.Context;
import com.callassistant.data.sync.SmsSyncer;
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
public final class AppModule_ProvideSmsSyncerFactory implements Factory<SmsSyncer> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideSmsSyncerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SmsSyncer get() {
    return provideSmsSyncer(contextProvider.get());
  }

  public static AppModule_ProvideSmsSyncerFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideSmsSyncerFactory(contextProvider);
  }

  public static SmsSyncer provideSmsSyncer(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSmsSyncer(context));
  }
}
