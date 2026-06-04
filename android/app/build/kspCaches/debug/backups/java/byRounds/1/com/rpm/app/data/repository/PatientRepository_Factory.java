package com.rpm.app.data.repository;

import com.rpm.app.data.local.TokenDataStore;
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
public final class PatientRepository_Factory implements Factory<PatientRepository> {
  private final Provider<RpmApiService> apiProvider;

  private final Provider<TokenDataStore> tokenStoreProvider;

  public PatientRepository_Factory(Provider<RpmApiService> apiProvider,
      Provider<TokenDataStore> tokenStoreProvider) {
    this.apiProvider = apiProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public PatientRepository get() {
    return newInstance(apiProvider.get(), tokenStoreProvider.get());
  }

  public static PatientRepository_Factory create(Provider<RpmApiService> apiProvider,
      Provider<TokenDataStore> tokenStoreProvider) {
    return new PatientRepository_Factory(apiProvider, tokenStoreProvider);
  }

  public static PatientRepository newInstance(RpmApiService api, TokenDataStore tokenStore) {
    return new PatientRepository(api, tokenStore);
  }
}
