package com.rpm.app.data.repository;

import com.rpm.app.data.auth.SessionManager;
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

  private final Provider<SessionManager> sessionManagerProvider;

  public NotificationRepository_Factory(Provider<RpmApiService> apiProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.apiProvider = apiProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public NotificationRepository get() {
    return newInstance(apiProvider.get(), sessionManagerProvider.get());
  }

  public static NotificationRepository_Factory create(Provider<RpmApiService> apiProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new NotificationRepository_Factory(apiProvider, sessionManagerProvider);
  }

  public static NotificationRepository newInstance(RpmApiService api,
      SessionManager sessionManager) {
    return new NotificationRepository(api, sessionManager);
  }
}
