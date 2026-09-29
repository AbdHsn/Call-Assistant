package com.callassistant;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.callassistant.ai.config.AzureOpenAiConfig;
import com.callassistant.ai.engine.AzureOpenAiInferenceEngine;
import com.callassistant.ai.engine.LlmInferenceEngine;
import com.callassistant.ai.engine.NativeLlamaInferenceEngine;
import com.callassistant.ai.engine.StubLlmInferenceEngine;
import com.callassistant.ai.prompt.MessagePromptBuilder;
import com.callassistant.data.db.AppDatabase;
import com.callassistant.data.db.BlockedNumberDao;
import com.callassistant.data.db.SpamRuleDao;
import com.callassistant.data.repository.AiModelRepository;
import com.callassistant.data.repository.AiModelRepositoryImpl;
import com.callassistant.data.repository.CallLogRepositoryImpl;
import com.callassistant.data.repository.ContactRepositoryImpl;
import com.callassistant.data.repository.NotesRepositoryImpl;
import com.callassistant.data.repository.SettingsRepositoryImpl;
import com.callassistant.data.repository.SmsRepository;
import com.callassistant.data.repository.SmsRepositoryImpl;
import com.callassistant.data.repository.SpamRuleRepository;
import com.callassistant.data.repository.SpamRuleRepositoryImpl;
import com.callassistant.data.sync.CallLogSyncer;
import com.callassistant.data.sync.ContactSyncer;
import com.callassistant.data.sync.SmsSyncer;
import com.callassistant.di.AiModule_Companion_ProvideLlmInferenceEngineFactory;
import com.callassistant.di.AppModule_ProvideAppDatabaseFactory;
import com.callassistant.di.AppModule_ProvideBlockedNumberDaoFactory;
import com.callassistant.di.AppModule_ProvideCallLogSyncerFactory;
import com.callassistant.di.AppModule_ProvideCallSessionManagerFactory;
import com.callassistant.di.AppModule_ProvideContactSyncerFactory;
import com.callassistant.di.AppModule_ProvideSmsSyncerFactory;
import com.callassistant.di.AppModule_ProvideSpamRuleDaoFactory;
import com.callassistant.incall.CallAssistantInCallService;
import com.callassistant.incall.CallAssistantInCallService_MembersInjector;
import com.callassistant.incall.CallSessionManager;
import com.callassistant.incall.InCallActivity;
import com.callassistant.incall.InCallActivity_MembersInjector;
import com.callassistant.service.CallScreeningServiceImpl;
import com.callassistant.service.CallScreeningServiceImpl_MembersInjector;
import com.callassistant.sms.SmsActivity;
import com.callassistant.ui.MainViewModel;
import com.callassistant.ui.MainViewModel_HiltModules;
import com.callassistant.ui.MessagesViewModel;
import com.callassistant.ui.MessagesViewModel_HiltModules;
import com.callassistant.ui.SpamRulesViewModel;
import com.callassistant.ui.SpamRulesViewModel_HiltModules;
import com.callassistant.ui.messageai.MessageAiViewModel;
import com.callassistant.ui.messageai.MessageAiViewModel_HiltModules;
import com.callassistant.ui.notes.NotesViewModel;
import com.callassistant.ui.notes.NotesViewModel_HiltModules;
import com.callassistant.ui.phonebook.PhoneBookViewModel;
import com.callassistant.ui.phonebook.PhoneBookViewModel_HiltModules;
import com.callassistant.ui.recordings.RecordingsViewModel;
import com.callassistant.ui.recordings.RecordingsViewModel_HiltModules;
import com.callassistant.util.NetworkConnectivityMonitor;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerCallAssistantApplication_HiltComponents_SingletonC {
  private DaggerCallAssistantApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public CallAssistantApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements CallAssistantApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public CallAssistantApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements CallAssistantApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public CallAssistantApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements CallAssistantApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public CallAssistantApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements CallAssistantApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CallAssistantApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements CallAssistantApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CallAssistantApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements CallAssistantApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public CallAssistantApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements CallAssistantApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public CallAssistantApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends CallAssistantApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends CallAssistantApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends CallAssistantApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends CallAssistantApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public void injectInCallActivity(InCallActivity inCallActivity) {
      injectInCallActivity2(inCallActivity);
    }

    @Override
    public void injectSmsActivity(SmsActivity smsActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(7).put(LazyClassKeyProvider.com_callassistant_ui_MainViewModel, MainViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_callassistant_ui_messageai_MessageAiViewModel, MessageAiViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_callassistant_ui_MessagesViewModel, MessagesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_callassistant_ui_notes_NotesViewModel, NotesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_callassistant_ui_phonebook_PhoneBookViewModel, PhoneBookViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_callassistant_ui_recordings_RecordingsViewModel, RecordingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_callassistant_ui_SpamRulesViewModel, SpamRulesViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private InCallActivity injectInCallActivity2(InCallActivity instance) {
      InCallActivity_MembersInjector.injectSession(instance, singletonCImpl.provideCallSessionManagerProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_callassistant_ui_MainViewModel = "com.callassistant.ui.MainViewModel";

      static String com_callassistant_ui_phonebook_PhoneBookViewModel = "com.callassistant.ui.phonebook.PhoneBookViewModel";

      static String com_callassistant_ui_messageai_MessageAiViewModel = "com.callassistant.ui.messageai.MessageAiViewModel";

      static String com_callassistant_ui_MessagesViewModel = "com.callassistant.ui.MessagesViewModel";

      static String com_callassistant_ui_SpamRulesViewModel = "com.callassistant.ui.SpamRulesViewModel";

      static String com_callassistant_ui_recordings_RecordingsViewModel = "com.callassistant.ui.recordings.RecordingsViewModel";

      static String com_callassistant_ui_notes_NotesViewModel = "com.callassistant.ui.notes.NotesViewModel";

      @KeepFieldType
      MainViewModel com_callassistant_ui_MainViewModel2;

      @KeepFieldType
      PhoneBookViewModel com_callassistant_ui_phonebook_PhoneBookViewModel2;

      @KeepFieldType
      MessageAiViewModel com_callassistant_ui_messageai_MessageAiViewModel2;

      @KeepFieldType
      MessagesViewModel com_callassistant_ui_MessagesViewModel2;

      @KeepFieldType
      SpamRulesViewModel com_callassistant_ui_SpamRulesViewModel2;

      @KeepFieldType
      RecordingsViewModel com_callassistant_ui_recordings_RecordingsViewModel2;

      @KeepFieldType
      NotesViewModel com_callassistant_ui_notes_NotesViewModel2;
    }
  }

  private static final class ViewModelCImpl extends CallAssistantApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<MainViewModel> mainViewModelProvider;

    private Provider<MessageAiViewModel> messageAiViewModelProvider;

    private Provider<MessagesViewModel> messagesViewModelProvider;

    private Provider<NotesViewModel> notesViewModelProvider;

    private Provider<PhoneBookViewModel> phoneBookViewModelProvider;

    private Provider<RecordingsViewModel> recordingsViewModelProvider;

    private Provider<SpamRulesViewModel> spamRulesViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.mainViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.messageAiViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.messagesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.notesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.phoneBookViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.recordingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.spamRulesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(7).put(LazyClassKeyProvider.com_callassistant_ui_MainViewModel, ((Provider) mainViewModelProvider)).put(LazyClassKeyProvider.com_callassistant_ui_messageai_MessageAiViewModel, ((Provider) messageAiViewModelProvider)).put(LazyClassKeyProvider.com_callassistant_ui_MessagesViewModel, ((Provider) messagesViewModelProvider)).put(LazyClassKeyProvider.com_callassistant_ui_notes_NotesViewModel, ((Provider) notesViewModelProvider)).put(LazyClassKeyProvider.com_callassistant_ui_phonebook_PhoneBookViewModel, ((Provider) phoneBookViewModelProvider)).put(LazyClassKeyProvider.com_callassistant_ui_recordings_RecordingsViewModel, ((Provider) recordingsViewModelProvider)).put(LazyClassKeyProvider.com_callassistant_ui_SpamRulesViewModel, ((Provider) spamRulesViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_callassistant_ui_messageai_MessageAiViewModel = "com.callassistant.ui.messageai.MessageAiViewModel";

      static String com_callassistant_ui_MainViewModel = "com.callassistant.ui.MainViewModel";

      static String com_callassistant_ui_MessagesViewModel = "com.callassistant.ui.MessagesViewModel";

      static String com_callassistant_ui_notes_NotesViewModel = "com.callassistant.ui.notes.NotesViewModel";

      static String com_callassistant_ui_recordings_RecordingsViewModel = "com.callassistant.ui.recordings.RecordingsViewModel";

      static String com_callassistant_ui_phonebook_PhoneBookViewModel = "com.callassistant.ui.phonebook.PhoneBookViewModel";

      static String com_callassistant_ui_SpamRulesViewModel = "com.callassistant.ui.SpamRulesViewModel";

      @KeepFieldType
      MessageAiViewModel com_callassistant_ui_messageai_MessageAiViewModel2;

      @KeepFieldType
      MainViewModel com_callassistant_ui_MainViewModel2;

      @KeepFieldType
      MessagesViewModel com_callassistant_ui_MessagesViewModel2;

      @KeepFieldType
      NotesViewModel com_callassistant_ui_notes_NotesViewModel2;

      @KeepFieldType
      RecordingsViewModel com_callassistant_ui_recordings_RecordingsViewModel2;

      @KeepFieldType
      PhoneBookViewModel com_callassistant_ui_phonebook_PhoneBookViewModel2;

      @KeepFieldType
      SpamRulesViewModel com_callassistant_ui_SpamRulesViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.callassistant.ui.MainViewModel 
          return (T) new MainViewModel(singletonCImpl.settingsRepositoryImplProvider.get());

          case 1: // com.callassistant.ui.messageai.MessageAiViewModel 
          return (T) new MessageAiViewModel(singletonCImpl.aiModelRepositoryImplProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get(), singletonCImpl.provideLlmInferenceEngineProvider.get(), singletonCImpl.nativeLlamaInferenceEngineProvider.get(), singletonCImpl.azureOpenAiInferenceEngineProvider.get(), singletonCImpl.azureOpenAiConfigProvider.get(), singletonCImpl.stubLlmInferenceEngineProvider.get(), singletonCImpl.messagePromptBuilderProvider.get(), singletonCImpl.networkConnectivityMonitorProvider.get());

          case 2: // com.callassistant.ui.MessagesViewModel 
          return (T) new MessagesViewModel(singletonCImpl.smsRepositoryImplProvider.get(), singletonCImpl.contactRepositoryImplProvider.get());

          case 3: // com.callassistant.ui.notes.NotesViewModel 
          return (T) new NotesViewModel(singletonCImpl.contactRepositoryImplProvider.get(), singletonCImpl.notesRepositoryImplProvider.get());

          case 4: // com.callassistant.ui.phonebook.PhoneBookViewModel 
          return (T) new PhoneBookViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.contactRepositoryImplProvider.get(), singletonCImpl.callLogRepositoryImplProvider.get(), singletonCImpl.spamRuleRepositoryImplProvider.get());

          case 5: // com.callassistant.ui.recordings.RecordingsViewModel 
          return (T) new RecordingsViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.callassistant.ui.SpamRulesViewModel 
          return (T) new SpamRulesViewModel(singletonCImpl.spamRuleRepositoryImplProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends CallAssistantApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends CallAssistantApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectCallAssistantInCallService(
        CallAssistantInCallService callAssistantInCallService) {
      injectCallAssistantInCallService2(callAssistantInCallService);
    }

    @Override
    public void injectCallScreeningServiceImpl(CallScreeningServiceImpl callScreeningServiceImpl) {
      injectCallScreeningServiceImpl2(callScreeningServiceImpl);
    }

    private CallAssistantInCallService injectCallAssistantInCallService2(
        CallAssistantInCallService instance) {
      CallAssistantInCallService_MembersInjector.injectSession(instance, singletonCImpl.provideCallSessionManagerProvider.get());
      return instance;
    }

    private CallScreeningServiceImpl injectCallScreeningServiceImpl2(
        CallScreeningServiceImpl instance) {
      CallScreeningServiceImpl_MembersInjector.injectRepo(instance, singletonCImpl.spamRuleRepositoryImplProvider.get());
      CallScreeningServiceImpl_MembersInjector.injectCallLogRepository(instance, singletonCImpl.callLogRepositoryImplProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends CallAssistantApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<AppDatabase> provideAppDatabaseProvider;

    private Provider<SpamRuleRepositoryImpl> spamRuleRepositoryImplProvider;

    private Provider<SettingsRepositoryImpl> settingsRepositoryImplProvider;

    private Provider<AiModelRepositoryImpl> aiModelRepositoryImplProvider;

    private Provider<SmsRepositoryImpl> smsRepositoryImplProvider;

    private Provider<CallSessionManager> provideCallSessionManagerProvider;

    private Provider<NativeLlamaInferenceEngine> nativeLlamaInferenceEngineProvider;

    private Provider<LlmInferenceEngine> provideLlmInferenceEngineProvider;

    private Provider<AzureOpenAiConfig> azureOpenAiConfigProvider;

    private Provider<AzureOpenAiInferenceEngine> azureOpenAiInferenceEngineProvider;

    private Provider<MessagePromptBuilder> messagePromptBuilderProvider;

    private Provider<StubLlmInferenceEngine> stubLlmInferenceEngineProvider;

    private Provider<NetworkConnectivityMonitor> networkConnectivityMonitorProvider;

    private Provider<ContactRepositoryImpl> contactRepositoryImplProvider;

    private Provider<NotesRepositoryImpl> notesRepositoryImplProvider;

    private Provider<CallLogRepositoryImpl> callLogRepositoryImplProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private SpamRuleDao spamRuleDao() {
      return AppModule_ProvideSpamRuleDaoFactory.provideSpamRuleDao(provideAppDatabaseProvider.get());
    }

    private BlockedNumberDao blockedNumberDao() {
      return AppModule_ProvideBlockedNumberDaoFactory.provideBlockedNumberDao(provideAppDatabaseProvider.get());
    }

    private SmsSyncer smsSyncer() {
      return AppModule_ProvideSmsSyncerFactory.provideSmsSyncer(ApplicationContextModule_ProvideContextFactory.provideContext(applicationContextModule));
    }

    private ContactSyncer contactSyncer() {
      return AppModule_ProvideContactSyncerFactory.provideContactSyncer(ApplicationContextModule_ProvideContextFactory.provideContext(applicationContextModule));
    }

    private CallLogSyncer callLogSyncer() {
      return AppModule_ProvideCallLogSyncerFactory.provideCallLogSyncer(ApplicationContextModule_ProvideContextFactory.provideContext(applicationContextModule));
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideAppDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 1));
      this.spamRuleRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SpamRuleRepositoryImpl>(singletonCImpl, 0));
      this.settingsRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepositoryImpl>(singletonCImpl, 3));
      this.aiModelRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AiModelRepositoryImpl>(singletonCImpl, 2));
      this.smsRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SmsRepositoryImpl>(singletonCImpl, 4));
      this.provideCallSessionManagerProvider = DoubleCheck.provider(new SwitchingProvider<CallSessionManager>(singletonCImpl, 5));
      this.nativeLlamaInferenceEngineProvider = DoubleCheck.provider(new SwitchingProvider<NativeLlamaInferenceEngine>(singletonCImpl, 7));
      this.provideLlmInferenceEngineProvider = DoubleCheck.provider(new SwitchingProvider<LlmInferenceEngine>(singletonCImpl, 6));
      this.azureOpenAiConfigProvider = DoubleCheck.provider(new SwitchingProvider<AzureOpenAiConfig>(singletonCImpl, 9));
      this.azureOpenAiInferenceEngineProvider = DoubleCheck.provider(new SwitchingProvider<AzureOpenAiInferenceEngine>(singletonCImpl, 8));
      this.messagePromptBuilderProvider = DoubleCheck.provider(new SwitchingProvider<MessagePromptBuilder>(singletonCImpl, 11));
      this.stubLlmInferenceEngineProvider = DoubleCheck.provider(new SwitchingProvider<StubLlmInferenceEngine>(singletonCImpl, 10));
      this.networkConnectivityMonitorProvider = DoubleCheck.provider(new SwitchingProvider<NetworkConnectivityMonitor>(singletonCImpl, 12));
      this.contactRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ContactRepositoryImpl>(singletonCImpl, 13));
      this.notesRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<NotesRepositoryImpl>(singletonCImpl, 14));
      this.callLogRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<CallLogRepositoryImpl>(singletonCImpl, 15));
    }

    @Override
    public void injectCallAssistantApplication(CallAssistantApplication callAssistantApplication) {
      injectCallAssistantApplication2(callAssistantApplication);
    }

    @Override
    public SpamRuleRepository spamRuleRepository() {
      return spamRuleRepositoryImplProvider.get();
    }

    @Override
    public SmsRepository smsRepository() {
      return smsRepositoryImplProvider.get();
    }

    @Override
    public CallSessionManager callSessionManager() {
      return provideCallSessionManagerProvider.get();
    }

    @Override
    public AiModelRepository aiModelRepository() {
      return aiModelRepositoryImplProvider.get();
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private CallAssistantApplication injectCallAssistantApplication2(
        CallAssistantApplication instance) {
      CallAssistantApplication_MembersInjector.injectSpamRuleRepository(instance, spamRuleRepositoryImplProvider.get());
      CallAssistantApplication_MembersInjector.injectAiModelRepository(instance, aiModelRepositoryImplProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.callassistant.data.repository.SpamRuleRepositoryImpl 
          return (T) new SpamRuleRepositoryImpl(singletonCImpl.spamRuleDao(), singletonCImpl.blockedNumberDao());

          case 1: // com.callassistant.data.db.AppDatabase 
          return (T) AppModule_ProvideAppDatabaseFactory.provideAppDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.callassistant.data.repository.AiModelRepositoryImpl 
          return (T) new AiModelRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.settingsRepositoryImplProvider.get());

          case 3: // com.callassistant.data.repository.SettingsRepositoryImpl 
          return (T) new SettingsRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.callassistant.data.repository.SmsRepositoryImpl 
          return (T) new SmsRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideAppDatabaseProvider.get(), singletonCImpl.smsSyncer());

          case 5: // com.callassistant.incall.CallSessionManager 
          return (T) AppModule_ProvideCallSessionManagerFactory.provideCallSessionManager();

          case 6: // com.callassistant.ai.engine.LlmInferenceEngine 
          return (T) AiModule_Companion_ProvideLlmInferenceEngineFactory.provideLlmInferenceEngine(singletonCImpl.nativeLlamaInferenceEngineProvider.get());

          case 7: // com.callassistant.ai.engine.NativeLlamaInferenceEngine 
          return (T) new NativeLlamaInferenceEngine(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.callassistant.ai.engine.AzureOpenAiInferenceEngine 
          return (T) new AzureOpenAiInferenceEngine(singletonCImpl.azureOpenAiConfigProvider.get());

          case 9: // com.callassistant.ai.config.AzureOpenAiConfig 
          return (T) new AzureOpenAiConfig();

          case 10: // com.callassistant.ai.engine.StubLlmInferenceEngine 
          return (T) new StubLlmInferenceEngine(singletonCImpl.messagePromptBuilderProvider.get());

          case 11: // com.callassistant.ai.prompt.MessagePromptBuilder 
          return (T) new MessagePromptBuilder();

          case 12: // com.callassistant.util.NetworkConnectivityMonitor 
          return (T) new NetworkConnectivityMonitor(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 13: // com.callassistant.data.repository.ContactRepositoryImpl 
          return (T) new ContactRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideAppDatabaseProvider.get(), singletonCImpl.contactSyncer());

          case 14: // com.callassistant.data.repository.NotesRepositoryImpl 
          return (T) new NotesRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 15: // com.callassistant.data.repository.CallLogRepositoryImpl 
          return (T) new CallLogRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideAppDatabaseProvider.get(), singletonCImpl.callLogSyncer());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
