# Samsung Health Sensor SDK (official rules + broad keep)
-keepparameternames
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
-keep public class com.samsung.android.service.health.tracking.data.DataPoint { public *; }
-keep public class com.samsung.android.service.health.tracking.data.HealthTrackerType { public *; }
-keep public class com.samsung.android.service.health.tracking.data.ValueKey { public *; }
-keep public class com.samsung.android.service.health.tracking.data.ValueKey$* { public *; }
-keep public class com.samsung.android.service.health.tracking.HealthTracker { public *; }
-keep public class com.samsung.android.service.health.tracking.HealthTracker$* { public *; }
-keep public class com.samsung.android.service.health.tracking.HealthTrackingService { public *; }
-keep public class com.samsung.android.service.health.tracking.ConnectionListener { public *; }
-keep public class com.samsung.android.service.health.tracking.HealthTrackerException { public *; }
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
-keep class com.rpm.watch.service.VitalsMonitorService { *; }
