package com.rpm.app.data.repository;

import com.rpm.app.data.local.TokenDataStore;
import com.rpm.app.data.remote.api.RpmApiService;
import com.rpm.app.data.remote.api.TokenRefresher;
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<RpmApiService> apiProvider;

  private final Provider<TokenDataStore> tokenStoreProvider;

  private final Provider<TokenRefresher> tokenRefresherProvider;

  public AuthRepository_Factory(Provider<RpmApiService> apiProvider,
      Provider<TokenDataStore> tokenStoreProvider,
      Provider<TokenRefresher> tokenRefresherProvider) {
    this.apiProvider = apiProvider;
    this.tokenStoreProvider = tokenStoreProvider;
    this.tokenRefresherProvider = tokenRefresherProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(apiProvider.get(), tokenStoreProvider.get(), tokenRefresherProvider.get());
  }

  public static AuthRepository_Factory create(Provider<RpmApiService> apiProvider,
      Provider<TokenDataStore> tokenStoreProvider,
      Provider<TokenRefresher> tokenRefresherProvider) {
    return new AuthRepository_Factory(apiProvider, tokenStoreProvider, tokenRefresherProvider);
  }

  public static AuthRepository newInstance(RpmApiService api, TokenDataStore tokenStore,
      TokenRefresher tokenRefresher) {
    return new AuthRepository(api, tokenStore, tokenRefresher);
  }
}
