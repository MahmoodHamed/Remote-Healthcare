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
public final class ChatRepository_Factory implements Factory<ChatRepository> {
  private final Provider<RpmApiService> apiProvider;

  public ChatRepository_Factory(Provider<RpmApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public ChatRepository get() {
    return newInstance(apiProvider.get());
  }

  public static ChatRepository_Factory create(Provider<RpmApiService> apiProvider) {
    return new ChatRepository_Factory(apiProvider);
  }

  public static ChatRepository newInstance(RpmApiService api) {
    return new ChatRepository(api);
  }
}
