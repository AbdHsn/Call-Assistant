package com.callassistant.ai.engine;

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
public final class NativeLlamaInferenceEngine_Factory implements Factory<NativeLlamaInferenceEngine> {
  private final Provider<Context> contextProvider;

  public NativeLlamaInferenceEngine_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NativeLlamaInferenceEngine get() {
    return newInstance(contextProvider.get());
  }

  public static NativeLlamaInferenceEngine_Factory create(Provider<Context> contextProvider) {
    return new NativeLlamaInferenceEngine_Factory(contextProvider);
  }

  public static NativeLlamaInferenceEngine newInstance(Context context) {
    return new NativeLlamaInferenceEngine(context);
  }
}
