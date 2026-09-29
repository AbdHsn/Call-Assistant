package com.callassistant.incall;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class InCallActivity_MembersInjector implements MembersInjector<InCallActivity> {
  private final Provider<CallSessionManager> sessionProvider;

  public InCallActivity_MembersInjector(Provider<CallSessionManager> sessionProvider) {
    this.sessionProvider = sessionProvider;
  }

  public static MembersInjector<InCallActivity> create(
      Provider<CallSessionManager> sessionProvider) {
    return new InCallActivity_MembersInjector(sessionProvider);
  }

  @Override
  public void injectMembers(InCallActivity instance) {
    injectSession(instance, sessionProvider.get());
  }

  @InjectedFieldSignature("com.callassistant.incall.InCallActivity.session")
  public static void injectSession(InCallActivity instance, CallSessionManager session) {
    instance.session = session;
  }
}
