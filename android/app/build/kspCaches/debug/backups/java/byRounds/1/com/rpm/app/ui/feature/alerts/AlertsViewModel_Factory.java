package com.rpm.app.ui.feature.alerts;

import androidx.lifecycle.SavedStateHandle;
import com.rpm.app.data.repository.AlertRepository;
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
public final class AlertsViewModel_Factory implements Factory<AlertsViewModel> {
  private final Provider<AlertRepository> repoProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public AlertsViewModel_Factory(Provider<AlertRepository> repoProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repoProvider = repoProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public AlertsViewModel get() {
    return newInstance(repoProvider.get(), savedStateHandleProvider.get());
  }

  public static AlertsViewModel_Factory create(Provider<AlertRepository> repoProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new AlertsViewModel_Factory(repoProvider, savedStateHandleProvider);
  }

  public static AlertsViewModel newInstance(AlertRepository repo,
      SavedStateHandle savedStateHandle) {
    return new AlertsViewModel(repo, savedStateHandle);
  }
}
