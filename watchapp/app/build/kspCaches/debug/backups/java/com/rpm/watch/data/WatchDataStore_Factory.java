package com.rpm.watch.data;

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
public final class WatchDataStore_Factory implements Factory<WatchDataStore> {
  private final Provider<Context> contextProvider;

  private WatchDataStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WatchDataStore get() {
    return newInstance(contextProvider.get());
  }

  public static WatchDataStore_Factory create(Provider<Context> contextProvider) {
    return new WatchDataStore_Factory(contextProvider);
  }

  public static WatchDataStore newInstance(Context context) {
    return new WatchDataStore(context);
  }
}
