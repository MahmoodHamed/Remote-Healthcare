package com.rpm.app.data.remote.api;

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
public final class TokenRefresher_Factory implements Factory<TokenRefresher> {
  private final Provider<TokenDataStore> tokenStoreProvider;

  public TokenRefresher_Factory(Provider<TokenDataStore> tokenStoreProvider) {
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public TokenRefresher get() {
    return newInstance(tokenStoreProvider.get());
  }

  public static TokenRefresher_Factory create(Provider<TokenDataStore> tokenStoreProvider) {
    return new TokenRefresher_Factory(tokenStoreProvider);
  }

  public static TokenRefresher newInstance(TokenDataStore tokenStore) {
    return new TokenRefresher(tokenStore);
  }
}
