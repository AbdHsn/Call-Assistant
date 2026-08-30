package com.callassistant.di;

import android.content.Context;
import com.callassistant.data.sync.CallLogSyncer;
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
public final class AppModule_ProvideCallLogSyncerFactory implements Factory<CallLogSyncer> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideCallLogSyncerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CallLogSyncer get() {
    return provideCallLogSyncer(contextProvider.get());
  }

  public static AppModule_ProvideCallLogSyncerFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideCallLogSyncerFactory(contextProvider);
  }

  public static CallLogSyncer provideCallLogSyncer(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCallLogSyncer(context));
  }
}
