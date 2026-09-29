package com.callassistant.data.repository;

import com.callassistant.data.db.BlockedNumberDao;
import com.callassistant.data.db.SpamRuleDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class SpamRuleRepositoryImpl_Factory implements Factory<SpamRuleRepositoryImpl> {
  private final Provider<SpamRuleDao> daoProvider;

  private final Provider<BlockedNumberDao> blockedNumberDaoProvider;

  public SpamRuleRepositoryImpl_Factory(Provider<SpamRuleDao> daoProvider,
      Provider<BlockedNumberDao> blockedNumberDaoProvider) {
    this.daoProvider = daoProvider;
    this.blockedNumberDaoProvider = blockedNumberDaoProvider;
  }

  @Override
  public SpamRuleRepositoryImpl get() {
    return newInstance(daoProvider.get(), blockedNumberDaoProvider.get());
  }

  public static SpamRuleRepositoryImpl_Factory create(Provider<SpamRuleDao> daoProvider,
      Provider<BlockedNumberDao> blockedNumberDaoProvider) {
    return new SpamRuleRepositoryImpl_Factory(daoProvider, blockedNumberDaoProvider);
  }

  public static SpamRuleRepositoryImpl newInstance(SpamRuleDao dao,
      BlockedNumberDao blockedNumberDao) {
    return new SpamRuleRepositoryImpl(dao, blockedNumberDao);
  }
}
