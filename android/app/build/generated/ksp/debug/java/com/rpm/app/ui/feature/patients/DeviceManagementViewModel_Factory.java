package com.rpm.app.ui.feature.patients;

import com.rpm.app.data.local.TokenDataStore;
import com.rpm.app.data.repository.DeviceRepository;
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
public final class DeviceManagementViewModel_Factory implements Factory<DeviceManagementViewModel> {
  private final Provider<DeviceRepository> repoProvider;

  private final Provider<TokenDataStore> tokenStoreProvider;

  public DeviceManagementViewModel_Factory(Provider<DeviceRepository> repoProvider,
      Provider<TokenDataStore> tokenStoreProvider) {
    this.repoProvider = repoProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public DeviceManagementViewModel get() {
    return newInstance(repoProvider.get(), tokenStoreProvider.get());
  }

  public static DeviceManagementViewModel_Factory create(Provider<DeviceRepository> repoProvider,
      Provider<TokenDataStore> tokenStoreProvider) {
    return new DeviceManagementViewModel_Factory(repoProvider, tokenStoreProvider);
  }

  public static DeviceManagementViewModel newInstance(DeviceRepository repo,
      TokenDataStore tokenStore) {
    return new DeviceManagementViewModel(repo, tokenStore);
  }
}
