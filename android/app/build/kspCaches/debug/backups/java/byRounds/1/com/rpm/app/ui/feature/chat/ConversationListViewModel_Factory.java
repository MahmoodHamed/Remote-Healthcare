package com.rpm.app.ui.feature.chat;

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
public final class ConversationListViewModel_Factory implements Factory<ConversationListViewModel> {
  private final Provider<ChatRepository> repoProvider;

  private final Provider<ChatSignalRClient> chatSignalRProvider;

  public ConversationListViewModel_Factory(Provider<ChatRepository> repoProvider,
      Provider<ChatSignalRClient> chatSignalRProvider) {
    this.repoProvider = repoProvider;
    this.chatSignalRProvider = chatSignalRProvider;
  }

  @Override
  public ConversationListViewModel get() {
    return newInstance(repoProvider.get(), chatSignalRProvider.get());
  }

  public static ConversationListViewModel_Factory create(Provider<ChatRepository> repoProvider,
      Provider<ChatSignalRClient> chatSignalRProvider) {
    return new ConversationListViewModel_Factory(repoProvider, chatSignalRProvider);
  }

  public static ConversationListViewModel newInstance(ChatRepository repo,
      ChatSignalRClient chatSignalR) {
    return new ConversationListViewModel(repo, chatSignalR);
  }
}
