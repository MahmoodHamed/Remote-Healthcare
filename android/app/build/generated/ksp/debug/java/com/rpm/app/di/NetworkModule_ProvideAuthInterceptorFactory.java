package com.rpm.app.di;

import com.rpm.app.data.local.TokenDataStore;
import com.rpm.app.data.remote.api.AuthInterceptor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class NetworkModule_ProvideAuthInterceptorFactory implements Factory<AuthInterceptor> {
  private final Provider<TokenDataStore> tokenStoreProvider;

  public NetworkModule_ProvideAuthInterceptorFactory(Provider<TokenDataStore> tokenStoreProvider) {
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public AuthInterceptor get() {
    return provideAuthInterceptor(tokenStoreProvider.get());
  }

  public static NetworkModule_ProvideAuthInterceptorFactory create(
      Provider<TokenDataStore> tokenStoreProvider) {
    return new NetworkModule_ProvideAuthInterceptorFactory(tokenStoreProvider);
  }

  public static AuthInterceptor provideAuthInterceptor(TokenDataStore tokenStore) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAuthInterceptor(tokenStore));
  }
}
