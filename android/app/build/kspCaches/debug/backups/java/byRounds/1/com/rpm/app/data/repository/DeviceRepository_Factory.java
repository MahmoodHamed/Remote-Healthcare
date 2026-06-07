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
public final class DeviceRepository_Factory implements Factory<DeviceRepository> {
  private final Provider<RpmApiService> apiProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<TokenDataStore> tokenStoreProvider;

  public DeviceRepository_Factory(Provider<RpmApiService> apiProvider,
      Provider<SessionManager> sessionManagerProvider,
      Provider<TokenDataStore> tokenStoreProvider) {
    this.apiProvider = apiProvider;
    this.sessionManagerProvider = sessionManagerProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public DeviceRepository get() {
    return newInstance(apiProvider.get(), sessionManagerProvider.get(), tokenStoreProvider.get());
  }

  public static DeviceRepository_Factory create(Provider<RpmApiService> apiProvider,
      Provider<SessionManager> sessionManagerProvider,
      Provider<TokenDataStore> tokenStoreProvider) {
    return new DeviceRepository_Factory(apiProvider, sessionManagerProvider, tokenStoreProvider);
  }

  public static DeviceRepository newInstance(RpmApiService api, SessionManager sessionManager,
      TokenDataStore tokenStore) {
    return new DeviceRepository(api, sessionManager, tokenStore);
  }
}
