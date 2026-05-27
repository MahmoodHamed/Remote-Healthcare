package com.rpm.app.ui.feature.chat;

import com.rpm.app.data.repository.ChatRepository;
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

  public ConversationListViewModel_Factory(Provider<ChatRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public ConversationListViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static ConversationListViewModel_Factory create(Provider<ChatRepository> repoProvider) {
    return new ConversationListViewModel_Factory(repoProvider);
  }

  public static ConversationListViewModel newInstance(ChatRepository repo) {
    return new ConversationListViewModel(repo);
  }
}
