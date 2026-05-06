package com.rpm.watch;

import android.content.Context;
import com.rpm.watch.data.WatchDataStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class WatchViewModel_Factory implements Factory<WatchViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<WatchDataStore> dataStoreProvider;

  private WatchViewModel_Factory(Provider<Context> contextProvider,
      Provider<WatchDataStore> dataStoreProvider) {
    this.contextProvider = contextProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public WatchViewModel get() {
    return newInstance(contextProvider.get(), dataStoreProvider.get());
  }

  public static WatchViewModel_Factory create(Provider<Context> contextProvider,
      Provider<WatchDataStore> dataStoreProvider) {
    return new WatchViewModel_Factory(contextProvider, dataStoreProvider);
  }

  public static WatchViewModel newInstance(Context context, WatchDataStore dataStore) {
    return new WatchViewModel(context, dataStore);
  }
}
