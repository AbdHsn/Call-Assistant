package com.callassistant.ai.prompt;

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
public final class MessagePromptBuilder_Factory implements Factory<MessagePromptBuilder> {
  @Override
  public MessagePromptBuilder get() {
    return newInstance();
  }

  public static MessagePromptBuilder_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MessagePromptBuilder newInstance() {
    return new MessagePromptBuilder();
  }

  private static final class InstanceHolder {
    private static final MessagePromptBuilder_Factory INSTANCE = new MessagePromptBuilder_Factory();
  }
}
