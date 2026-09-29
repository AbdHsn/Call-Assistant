package com.callassistant.ui.recordings;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RecordingsViewModel_Factory implements Factory<RecordingsViewModel> {
  private final Provider<Context> contextProvider;

  public RecordingsViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public RecordingsViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static RecordingsViewModel_Factory create(Provider<Context> contextProvider) {
    return new RecordingsViewModel_Factory(contextProvider);
  }

  public static RecordingsViewModel newInstance(Context context) {
    return new RecordingsViewModel(context);
  }
}
