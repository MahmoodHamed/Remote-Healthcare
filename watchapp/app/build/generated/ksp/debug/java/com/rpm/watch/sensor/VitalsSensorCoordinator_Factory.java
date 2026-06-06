package com.rpm.watch.sensor;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class VitalsSensorCoordinator_Factory implements Factory<VitalsSensorCoordinator> {
  private final Provider<Context> contextProvider;

  private VitalsSensorCoordinator_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public VitalsSensorCoordinator get() {
    return newInstance(contextProvider.get());
  }

  public static VitalsSensorCoordinator_Factory create(Provider<Context> contextProvider) {
    return new VitalsSensorCoordinator_Factory(contextProvider);
  }

  public static VitalsSensorCoordinator newInstance(Context context) {
    return new VitalsSensorCoordinator(context);
  }
}
