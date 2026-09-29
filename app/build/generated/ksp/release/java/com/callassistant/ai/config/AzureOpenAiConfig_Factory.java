package com.callassistant.ai.config;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class AzureOpenAiConfig_Factory implements Factory<AzureOpenAiConfig> {
  @Override
  public AzureOpenAiConfig get() {
    return newInstance();
  }

  public static AzureOpenAiConfig_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AzureOpenAiConfig newInstance() {
    return new AzureOpenAiConfig();
  }

  private static final class InstanceHolder {
    private static final AzureOpenAiConfig_Factory INSTANCE = new AzureOpenAiConfig_Factory();
  }
}
