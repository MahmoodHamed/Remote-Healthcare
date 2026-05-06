package com.rpm.watch.service;

import com.rpm.watch.data.WatchDataStore;
import com.rpm.watch.health.HeartRateTrackerManager;
import com.rpm.watch.mqtt.MqttManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class HeartRateMonitorService_MembersInjector implements MembersInjector<HeartRateMonitorService> {
  private final Provider<HeartRateTrackerManager> hrTrackerManagerProvider;

  private final Provider<MqttManager> mqttManagerProvider;

  private final Provider<WatchDataStore> dataStoreProvider;

  private HeartRateMonitorService_MembersInjector(
      Provider<HeartRateTrackerManager> hrTrackerManagerProvider,
      Provider<MqttManager> mqttManagerProvider, Provider<WatchDataStore> dataStoreProvider) {
    this.hrTrackerManagerProvider = hrTrackerManagerProvider;
    this.mqttManagerProvider = mqttManagerProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public void injectMembers(HeartRateMonitorService instance) {
    injectHrTrackerManager(instance, hrTrackerManagerProvider.get());
    injectMqttManager(instance, mqttManagerProvider.get());
    injectDataStore(instance, dataStoreProvider.get());
  }

  public static MembersInjector<HeartRateMonitorService> create(
      Provider<HeartRateTrackerManager> hrTrackerManagerProvider,
      Provider<MqttManager> mqttManagerProvider, Provider<WatchDataStore> dataStoreProvider) {
    return new HeartRateMonitorService_MembersInjector(hrTrackerManagerProvider, mqttManagerProvider, dataStoreProvider);
  }

  @InjectedFieldSignature("com.rpm.watch.service.HeartRateMonitorService.hrTrackerManager")
  public static void injectHrTrackerManager(HeartRateMonitorService instance,
      HeartRateTrackerManager hrTrackerManager) {
    instance.hrTrackerManager = hrTrackerManager;
  }

  @InjectedFieldSignature("com.rpm.watch.service.HeartRateMonitorService.mqttManager")
  public static void injectMqttManager(HeartRateMonitorService instance, MqttManager mqttManager) {
    instance.mqttManager = mqttManager;
  }

  @InjectedFieldSignature("com.rpm.watch.service.HeartRateMonitorService.dataStore")
  public static void injectDataStore(HeartRateMonitorService instance, WatchDataStore dataStore) {
    instance.dataStore = dataStore;
  }
}
