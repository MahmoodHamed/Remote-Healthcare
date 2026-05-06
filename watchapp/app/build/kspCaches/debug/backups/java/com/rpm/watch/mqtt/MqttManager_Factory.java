package com.rpm.watch.mqtt;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class MqttManager_Factory implements Factory<MqttManager> {
  @Override
  public MqttManager get() {
    return newInstance();
  }

  public static MqttManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MqttManager newInstance() {
    return new MqttManager();
  }

  private static final class InstanceHolder {
    static final MqttManager_Factory INSTANCE = new MqttManager_Factory();
  }
}
