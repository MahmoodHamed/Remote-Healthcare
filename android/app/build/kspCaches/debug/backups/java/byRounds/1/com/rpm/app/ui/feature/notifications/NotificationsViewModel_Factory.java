package com.rpm.app.ui.feature.notifications;

import com.rpm.app.data.repository.NotificationRepository;
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
public final class NotificationsViewModel_Factory implements Factory<NotificationsViewModel> {
  private final Provider<NotificationRepository> repoProvider;

  public NotificationsViewModel_Factory(Provider<NotificationRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public NotificationsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static NotificationsViewModel_Factory create(
      Provider<NotificationRepository> repoProvider) {
    return new NotificationsViewModel_Factory(repoProvider);
  }

  public static NotificationsViewModel newInstance(NotificationRepository repo) {
    return new NotificationsViewModel(repo);
  }
}
