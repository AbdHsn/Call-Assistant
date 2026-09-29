package com.callassistant.data.repository;

import android.content.Context;
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
public final class AiModelRepositoryImpl_Factory implements Factory<AiModelRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public AiModelRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public AiModelRepositoryImpl get() {
    return newInstance(contextProvider.get(), settingsRepositoryProvider.get());
  }

  public static AiModelRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new AiModelRepositoryImpl_Factory(contextProvider, settingsRepositoryProvider);
  }

  public static AiModelRepositoryImpl newInstance(Context context,
      SettingsRepository settingsRepository) {
    return new AiModelRepositoryImpl(context, settingsRepository);
  }
}
