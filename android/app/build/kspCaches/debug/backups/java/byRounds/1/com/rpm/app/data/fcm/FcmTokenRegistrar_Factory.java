package com.rpm.app.data.fcm;

import com.rpm.app.data.remote.api.RpmApiService;
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
public final class FcmTokenRegistrar_Factory implements Factory<FcmTokenRegistrar> {
  private final Provider<RpmApiService> apiProvider;

  public FcmTokenRegistrar_Factory(Provider<RpmApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public FcmTokenRegistrar get() {
    return newInstance(apiProvider.get());
  }

  public static FcmTokenRegistrar_Factory create(Provider<RpmApiService> apiProvider) {
    return new FcmTokenRegistrar_Factory(apiProvider);
  }

  public static FcmTokenRegistrar newInstance(RpmApiService api) {
    return new FcmTokenRegistrar(api);
  }
}
