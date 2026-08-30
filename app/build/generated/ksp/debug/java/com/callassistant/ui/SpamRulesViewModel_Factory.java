package com.callassistant.ui;

import com.callassistant.data.repository.SpamRuleRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SpamRulesViewModel_Factory implements Factory<SpamRulesViewModel> {
  private final Provider<SpamRuleRepository> spamRuleRepositoryProvider;

  public SpamRulesViewModel_Factory(Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    this.spamRuleRepositoryProvider = spamRuleRepositoryProvider;
  }

  @Override
  public SpamRulesViewModel get() {
    return newInstance(spamRuleRepositoryProvider.get());
  }

  public static SpamRulesViewModel_Factory create(
      Provider<SpamRuleRepository> spamRuleRepositoryProvider) {
    return new SpamRulesViewModel_Factory(spamRuleRepositoryProvider);
  }

  public static SpamRulesViewModel newInstance(SpamRuleRepository spamRuleRepository) {
    return new SpamRulesViewModel(spamRuleRepository);
  }
}
