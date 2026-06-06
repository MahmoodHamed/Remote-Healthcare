package com.rpm.app.ui.feature.patients;

import androidx.lifecycle.SavedStateHandle;
import com.rpm.app.data.repository.PatientRepository;
import com.rpm.app.data.signalr.VitalsSignalRClient;
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
public final class LiveMonitorViewModel_Factory implements Factory<LiveMonitorViewModel> {
  private final Provider<PatientRepository> repoProvider;

  private final Provider<VitalsSignalRClient> signalRProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public LiveMonitorViewModel_Factory(Provider<PatientRepository> repoProvider,
      Provider<VitalsSignalRClient> signalRProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repoProvider = repoProvider;
    this.signalRProvider = signalRProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public LiveMonitorViewModel get() {
    return newInstance(repoProvider.get(), signalRProvider.get(), savedStateHandleProvider.get());
  }

  public static LiveMonitorViewModel_Factory create(Provider<PatientRepository> repoProvider,
      Provider<VitalsSignalRClient> signalRProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new LiveMonitorViewModel_Factory(repoProvider, signalRProvider, savedStateHandleProvider);
  }

  public static LiveMonitorViewModel newInstance(PatientRepository repo,
      VitalsSignalRClient signalR, SavedStateHandle savedStateHandle) {
    return new LiveMonitorViewModel(repo, signalR, savedStateHandle);
  }
}
