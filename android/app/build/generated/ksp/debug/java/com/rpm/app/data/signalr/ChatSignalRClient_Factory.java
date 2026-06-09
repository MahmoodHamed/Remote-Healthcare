package com.rpm.app.data.signalr;

import com.rpm.app.data.local.TokenDataStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ChatSignalRClient_Factory implements Factory<ChatSignalRClient> {
  private final Provider<TokenDataStore> tokenStoreProvider;

  public ChatSignalRClient_Factory(Provider<TokenDataStore> tokenStoreProvider) {
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public ChatSignalRClient get() {
    return newInstance(tokenStoreProvider.get());
  }

  public static ChatSignalRClient_Factory create(Provider<TokenDataStore> tokenStoreProvider) {
    return new ChatSignalRClient_Factory(tokenStoreProvider);
  }

  public static ChatSignalRClient newInstance(TokenDataStore tokenStore) {
    return new ChatSignalRClient(tokenStore);
  }
}
