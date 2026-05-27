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
public final class AlertRepository_Factory implements Factory<AlertRepository> {
  private final Provider<RpmApiService> apiProvider;

  public AlertRepository_Factory(Provider<RpmApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public AlertRepository get() {
    return newInstance(apiProvider.get());
  }

  public static AlertRepository_Factory create(Provider<RpmApiService> apiProvider) {
    return new AlertRepository_Factory(apiProvider);
  }

  public static AlertRepository newInstance(RpmApiService api) {
    return new AlertRepository(api);
  }
}
