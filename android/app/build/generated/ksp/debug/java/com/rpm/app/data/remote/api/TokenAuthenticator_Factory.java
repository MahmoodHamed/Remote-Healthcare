package com.rpm.app.data.remote.api;

import com.rpm.app.data.auth.SessionManager;
import com.rpm.app.data.local.TokenDataStore;
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
public final class TokenAuthenticator_Factory implements Factory<TokenAuthenticator> {
  private final Provider<TokenRefresher> tokenRefresherProvider;

  private final Provider<TokenDataStore> tokenStoreProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public TokenAuthenticator_Factory(Provider<TokenRefresher> tokenRefresherProvider,
      Provider<TokenDataStore> tokenStoreProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.tokenRefresherProvider = tokenRefresherProvider;
    this.tokenStoreProvider = tokenStoreProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public TokenAuthenticator get() {
    return newInstance(tokenRefresherProvider.get(), tokenStoreProvider.get(), sessionManagerProvider.get());
  }

  public static TokenAuthenticator_Factory create(Provider<TokenRefresher> tokenRefresherProvider,
      Provider<TokenDataStore> tokenStoreProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new TokenAuthenticator_Factory(tokenRefresherProvider, tokenStoreProvider, sessionManagerProvider);
  }

  public static TokenAuthenticator newInstance(TokenRefresher tokenRefresher,
      TokenDataStore tokenStore, SessionManager sessionManager) {
    return new TokenAuthenticator(tokenRefresher, tokenStore, sessionManager);
  }
}
