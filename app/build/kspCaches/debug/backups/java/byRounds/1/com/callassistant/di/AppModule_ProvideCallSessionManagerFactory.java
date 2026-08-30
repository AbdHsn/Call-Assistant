package com.callassistant.di;

import com.callassistant.incall.CallSessionManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideCallSessionManagerFactory implements Factory<CallSessionManager> {
  @Override
  public CallSessionManager get() {
    return provideCallSessionManager();
  }

  public static AppModule_ProvideCallSessionManagerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CallSessionManager provideCallSessionManager() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCallSessionManager());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideCallSessionManagerFactory INSTANCE = new AppModule_ProvideCallSessionManagerFactory();
  }
}
