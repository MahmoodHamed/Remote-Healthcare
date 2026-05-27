package com.rpm.app.ui.feature.auth;

import com.rpm.app.data.local.TokenDataStore;
import com.rpm.app.data.repository.AuthRepository;
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<TokenDataStore> tokenStoreProvider;

  public AuthViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<TokenDataStore> tokenStoreProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(authRepositoryProvider.get(), tokenStoreProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<TokenDataStore> tokenStoreProvider) {
    return new AuthViewModel_Factory(authRepositoryProvider, tokenStoreProvider);
  }

  public static AuthViewModel newInstance(AuthRepository authRepository,
      TokenDataStore tokenStore) {
    return new AuthViewModel(authRepository, tokenStore);
  }
}
