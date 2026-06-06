package com.rpm.app.data.repository;

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
public final class NotificationRepository_Factory implements Factory<NotificationRepository> {
  private final Provider<RpmApiService> apiProvider;

  public NotificationRepository_Factory(Provider<RpmApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public NotificationRepository get() {
    return newInstance(apiProvider.get());
  }

  public static NotificationRepository_Factory create(Provider<RpmApiService> apiProvider) {
    return new NotificationRepository_Factory(apiProvider);
  }

  public static NotificationRepository newInstance(RpmApiService api) {
    return new NotificationRepository(api);
  }
}
