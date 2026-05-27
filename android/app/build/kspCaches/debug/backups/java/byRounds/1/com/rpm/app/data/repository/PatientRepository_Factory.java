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
public final class PatientRepository_Factory implements Factory<PatientRepository> {
  private final Provider<RpmApiService> apiProvider;

  public PatientRepository_Factory(Provider<RpmApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public PatientRepository get() {
    return newInstance(apiProvider.get());
  }

  public static PatientRepository_Factory create(Provider<RpmApiService> apiProvider) {
    return new PatientRepository_Factory(apiProvider);
  }

  public static PatientRepository newInstance(RpmApiService api) {
    return new PatientRepository(api);
  }
}
