package com.rpm.watch.di;

import android.content.Context;
import com.rpm.watch.data.WatchDataStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class WatchModule_ProvideWatchDataStoreFactory implements Factory<WatchDataStore> {
  private final Provider<Context> contextProvider;

  private WatchModule_ProvideWatchDataStoreFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WatchDataStore get() {
    return provideWatchDataStore(contextProvider.get());
  }

  public static WatchModule_ProvideWatchDataStoreFactory create(Provider<Context> contextProvider) {
    return new WatchModule_ProvideWatchDataStoreFactory(contextProvider);
  }

  public static WatchDataStore provideWatchDataStore(Context context) {
    return Preconditions.checkNotNullFromProvides(WatchModule.INSTANCE.provideWatchDataStore(context));
  }
}
