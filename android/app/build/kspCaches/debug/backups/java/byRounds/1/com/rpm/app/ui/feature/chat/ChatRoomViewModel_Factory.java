package com.rpm.app.ui.feature.chat;

import androidx.lifecycle.SavedStateHandle;
import com.rpm.app.data.repository.ChatRepository;
import com.rpm.app.data.signalr.ChatSignalRClient;
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
public final class ChatRoomViewModel_Factory implements Factory<ChatRoomViewModel> {
  private final Provider<ChatRepository> repoProvider;

  private final Provider<ChatSignalRClient> chatSignalRProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public ChatRoomViewModel_Factory(Provider<ChatRepository> repoProvider,
      Provider<ChatSignalRClient> chatSignalRProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repoProvider = repoProvider;
    this.chatSignalRProvider = chatSignalRProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public ChatRoomViewModel get() {
    return newInstance(repoProvider.get(), chatSignalRProvider.get(), savedStateHandleProvider.get());
  }

  public static ChatRoomViewModel_Factory create(Provider<ChatRepository> repoProvider,
      Provider<ChatSignalRClient> chatSignalRProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ChatRoomViewModel_Factory(repoProvider, chatSignalRProvider, savedStateHandleProvider);
  }

  public static ChatRoomViewModel newInstance(ChatRepository repo, ChatSignalRClient chatSignalR,
      SavedStateHandle savedStateHandle) {
    return new ChatRoomViewModel(repo, chatSignalR, savedStateHandle);
  }
}
