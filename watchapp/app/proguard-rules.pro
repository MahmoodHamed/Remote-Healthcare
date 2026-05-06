# Samsung Health Sensor SDK
-keep class com.samsung.android.service.health.** { *; }
-dontwarn com.samsung.android.service.health.**

# HiveMQ MQTT client
-keep class com.hivemq.client.** { *; }
-dontwarn com.hivemq.client.**

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Keep service
-keep class com.rpm.watch.service.HeartRateMonitorService { *; }
