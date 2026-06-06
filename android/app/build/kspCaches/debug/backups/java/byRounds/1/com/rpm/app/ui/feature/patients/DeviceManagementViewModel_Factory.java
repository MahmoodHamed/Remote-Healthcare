package com.rpm.app.ui.feature.patients;

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

  public DeviceManagementViewModel_Factory(Provider<DeviceRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DeviceManagementViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static DeviceManagementViewModel_Factory create(Provider<DeviceRepository> repoProvider) {
    return new DeviceManagementViewModel_Factory(repoProvider);
  }

  public static DeviceManagementViewModel newInstance(DeviceRepository repo) {
    return new DeviceManagementViewModel(repo);
  }
}
