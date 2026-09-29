package com.callassistant.util;

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
public final class NetworkConnectivityMonitor_Factory implements Factory<NetworkConnectivityMonitor> {
  private final Provider<Context> contextProvider;

  public NetworkConnectivityMonitor_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NetworkConnectivityMonitor get() {
    return newInstance(contextProvider.get());
  }

  public static NetworkConnectivityMonitor_Factory create(Provider<Context> contextProvider) {
    return new NetworkConnectivityMonitor_Factory(contextProvider);
  }

  public static NetworkConnectivityMonitor newInstance(Context context) {
    return new NetworkConnectivityMonitor(context);
  }
}
