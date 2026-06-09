package com.rpm.app.ui.feature.patients;

import androidx.lifecycle.SavedStateHandle;
import com.rpm.app.data.local.TokenDataStore;
import com.rpm.app.data.repository.ChatRepository;
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
public final class PatientDetailViewModel_Factory implements Factory<PatientDetailViewModel> {
  private final Provider<PatientRepository> repoProvider;

  private final Provider<ChatRepository> chatRepoProvider;

  private final Provider<TokenDataStore> tokenStoreProvider;

  private final Provider<VitalsSignalRClient> signalRProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public PatientDetailViewModel_Factory(Provider<PatientRepository> repoProvider,
      Provider<ChatRepository> chatRepoProvider, Provider<TokenDataStore> tokenStoreProvider,
      Provider<VitalsSignalRClient> signalRProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repoProvider = repoProvider;
    this.chatRepoProvider = chatRepoProvider;
    this.tokenStoreProvider = tokenStoreProvider;
    this.signalRProvider = signalRProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public PatientDetailViewModel get() {
    return newInstance(repoProvider.get(), chatRepoProvider.get(), tokenStoreProvider.get(), signalRProvider.get(), savedStateHandleProvider.get());
  }

  public static PatientDetailViewModel_Factory create(Provider<PatientRepository> repoProvider,
      Provider<ChatRepository> chatRepoProvider, Provider<TokenDataStore> tokenStoreProvider,
      Provider<VitalsSignalRClient> signalRProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new PatientDetailViewModel_Factory(repoProvider, chatRepoProvider, tokenStoreProvider, signalRProvider, savedStateHandleProvider);
  }

  public static PatientDetailViewModel newInstance(PatientRepository repo, ChatRepository chatRepo,
      TokenDataStore tokenStore, VitalsSignalRClient signalR, SavedStateHandle savedStateHandle) {
    return new PatientDetailViewModel(repo, chatRepo, tokenStore, signalR, savedStateHandle);
  }
}
