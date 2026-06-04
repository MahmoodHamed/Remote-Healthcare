package com.rpm.watch.service;

import com.rpm.watch.data.WatchDataStore;
import com.rpm.watch.mqtt.MqttManager;
import com.rpm.watch.sensor.VitalsSensorCoordinator;
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
public final class VitalsMonitorService_MembersInjector implements MembersInjector<VitalsMonitorService> {
  private final Provider<VitalsSensorCoordinator> vitalsCoordinatorProvider;

  private final Provider<MqttManager> mqttManagerProvider;

  private final Provider<WatchDataStore> dataStoreProvider;

  private VitalsMonitorService_MembersInjector(
      Provider<VitalsSensorCoordinator> vitalsCoordinatorProvider,
      Provider<MqttManager> mqttManagerProvider, Provider<WatchDataStore> dataStoreProvider) {
    this.vitalsCoordinatorProvider = vitalsCoordinatorProvider;
    this.mqttManagerProvider = mqttManagerProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public void injectMembers(VitalsMonitorService instance) {
    injectVitalsCoordinator(instance, vitalsCoordinatorProvider.get());
    injectMqttManager(instance, mqttManagerProvider.get());
    injectDataStore(instance, dataStoreProvider.get());
  }

  public static MembersInjector<VitalsMonitorService> create(
      Provider<VitalsSensorCoordinator> vitalsCoordinatorProvider,
      Provider<MqttManager> mqttManagerProvider, Provider<WatchDataStore> dataStoreProvider) {
    return new VitalsMonitorService_MembersInjector(vitalsCoordinatorProvider, mqttManagerProvider, dataStoreProvider);
  }

  @InjectedFieldSignature("com.rpm.watch.service.VitalsMonitorService.vitalsCoordinator")
  public static void injectVitalsCoordinator(VitalsMonitorService instance,
      VitalsSensorCoordinator vitalsCoordinator) {
    instance.vitalsCoordinator = vitalsCoordinator;
  }

  @InjectedFieldSignature("com.rpm.watch.service.VitalsMonitorService.mqttManager")
  public static void injectMqttManager(VitalsMonitorService instance, MqttManager mqttManager) {
    instance.mqttManager = mqttManager;
  }

  @InjectedFieldSignature("com.rpm.watch.service.VitalsMonitorService.dataStore")
  public static void injectDataStore(VitalsMonitorService instance, WatchDataStore dataStore) {
    instance.dataStore = dataStore;
  }
}
