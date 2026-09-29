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
public final class CallAssistantInCallService_MembersInjector implements MembersInjector<CallAssistantInCallService> {
  private final Provider<CallSessionManager> sessionProvider;

  public CallAssistantInCallService_MembersInjector(Provider<CallSessionManager> sessionProvider) {
    this.sessionProvider = sessionProvider;
  }

  public static MembersInjector<CallAssistantInCallService> create(
      Provider<CallSessionManager> sessionProvider) {
    return new CallAssistantInCallService_MembersInjector(sessionProvider);
  }

  @Override
  public void injectMembers(CallAssistantInCallService instance) {
    injectSession(instance, sessionProvider.get());
  }

  @InjectedFieldSignature("com.callassistant.incall.CallAssistantInCallService.session")
  public static void injectSession(CallAssistantInCallService instance,
      CallSessionManager session) {
    instance.session = session;
  }
}
