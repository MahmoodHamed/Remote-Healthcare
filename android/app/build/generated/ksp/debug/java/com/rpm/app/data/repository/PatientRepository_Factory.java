package com.rpm.app.data.repository;

import com.rpm.app.data.auth.SessionManager;
import com.rpm.app.data.local.TokenDataStore;
import com.rpm.app.data.remote.api.RpmApiService;
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
public final class PatientRepository_Factory implements Factory<PatientRepository> {
  private final Provider<RpmApiService> apiProvider;

  private final Provider<TokenDataStore> tokenStoreProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public PatientRepository_Factory(Provider<RpmApiService> apiProvider,
      Provider<TokenDataStore> tokenStoreProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.apiProvider = apiProvider;
    this.tokenStoreProvider = tokenStoreProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public PatientRepository get() {
    return newInstance(apiProvider.get(), tokenStoreProvider.get(), sessionManagerProvider.get());
  }

  public static PatientRepository_Factory create(Provider<RpmApiService> apiProvider,
      Provider<TokenDataStore> tokenStoreProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new PatientRepository_Factory(apiProvider, tokenStoreProvider, sessionManagerProvider);
  }

  public static PatientRepository newInstance(RpmApiService api, TokenDataStore tokenStore,
      SessionManager sessionManager) {
    return new PatientRepository(api, tokenStore, sessionManager);
  }
}
