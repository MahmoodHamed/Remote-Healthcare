package com.rpm.watch;

import android.content.Context;
import com.rpm.watch.data.WatchDataStore;
import com.rpm.watch.mqtt.MqttManager;
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

  private final Provider<MqttManager> mqttManagerProvider;

  private WatchViewModel_Factory(Provider<Context> contextProvider,
      Provider<WatchDataStore> dataStoreProvider, Provider<MqttManager> mqttManagerProvider) {
    this.contextProvider = contextProvider;
    this.dataStoreProvider = dataStoreProvider;
    this.mqttManagerProvider = mqttManagerProvider;
  }

  @Override
  public WatchViewModel get() {
    return newInstance(contextProvider.get(), dataStoreProvider.get(), mqttManagerProvider.get());
  }

  public static WatchViewModel_Factory create(Provider<Context> contextProvider,
      Provider<WatchDataStore> dataStoreProvider, Provider<MqttManager> mqttManagerProvider) {
    return new WatchViewModel_Factory(contextProvider, dataStoreProvider, mqttManagerProvider);
  }

  public static WatchViewModel newInstance(Context context, WatchDataStore dataStore,
      MqttManager mqttManager) {
    return new WatchViewModel(context, dataStore, mqttManager);
  }
}
