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
public final class NotesRepositoryImpl_Factory implements Factory<NotesRepositoryImpl> {
  private final Provider<Context> contextProvider;

  public NotesRepositoryImpl_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotesRepositoryImpl get() {
    return newInstance(contextProvider.get());
  }

  public static NotesRepositoryImpl_Factory create(Provider<Context> contextProvider) {
    return new NotesRepositoryImpl_Factory(contextProvider);
  }

  public static NotesRepositoryImpl newInstance(Context context) {
    return new NotesRepositoryImpl(context);
  }
}
