package com.rpm.app.ui.feature.auth;

import com.rpm.app.data.auth.SessionManager;
import com.rpm.app.data.fcm.FcmTokenRegistrar;
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

  private final Provider<FcmTokenRegistrar> fcmTokenRegistrarProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public AuthViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<TokenDataStore> tokenStoreProvider,
      Provider<FcmTokenRegistrar> fcmTokenRegistrarProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.tokenStoreProvider = tokenStoreProvider;
    this.fcmTokenRegistrarProvider = fcmTokenRegistrarProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(authRepositoryProvider.get(), tokenStoreProvider.get(), fcmTokenRegistrarProvider.get(), sessionManagerProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<TokenDataStore> tokenStoreProvider,
      Provider<FcmTokenRegistrar> fcmTokenRegistrarProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new AuthViewModel_Factory(authRepositoryProvider, tokenStoreProvider, fcmTokenRegistrarProvider, sessionManagerProvider);
  }

  public static AuthViewModel newInstance(AuthRepository authRepository, TokenDataStore tokenStore,
      FcmTokenRegistrar fcmTokenRegistrar, SessionManager sessionManager) {
    return new AuthViewModel(authRepository, tokenStore, fcmTokenRegistrar, sessionManager);
  }
}
