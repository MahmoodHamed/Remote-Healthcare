package com.rpm.watch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "watch_prefs")

/**
 * Persists all watch configuration:
 *  – patientId  : must match the User ID of the patient on the backend
 *  – deviceId   : a stable UUID for this watch (auto-generated on first run)
 *  – mqttHost   : MQTT broker address (falls back to BuildConfig.MQTT_HOST)
 *  – mqttPort   : MQTT broker port (falls back to BuildConfig.MQTT_PORT)
 */
@Singleton
class WatchDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val PATIENT_ID = stringPreferencesKey("patient_id")
    private val DEVICE_ID  = stringPreferencesKey("device_id")
    private val MQTT_HOST  = stringPreferencesKey("mqtt_host")
    private val MQTT_PORT  = intPreferencesKey("mqtt_port")

    val patientId: Flow<String?> = context.dataStore.data.map { it[PATIENT_ID] }
    val deviceId:  Flow<String>  = context.dataStore.data.map {
        it[DEVICE_ID] ?: UUID.randomUUID().toString()
    }
    val mqttHost: Flow<String> = context.dataStore.data.map {
        it[MQTT_HOST] ?: com.rpm.watch.BuildConfig.MQTT_HOST
    }
    val mqttPort: Flow<Int> = context.dataStore.data.map {
        it[MQTT_PORT] ?: com.rpm.watch.BuildConfig.MQTT_PORT
    }

    /** Returns current patientId synchronously (uses blocking first).
     *  For quick local testing, when running a DEBUG build this returns a
     *  temporary test id if none is stored. Remove or change for production.
     */
    suspend fun getPatientId(): String? {
        val stored = context.dataStore.data.first()[PATIENT_ID]
        if (!stored.isNullOrBlank()) return stored
        // Provide a debug fallback when running a debug build to allow fast testing
        return if (com.rpm.watch.BuildConfig.DEBUG) com.rpm.watch.BuildConfig.DEFAULT_PATIENT_ID else null
    }

    /** Returns current deviceId synchronously; auto-creates on first call. */
    suspend fun getOrCreateDeviceId(): String {
        val prefs = context.dataStore.data.first()
        return prefs[DEVICE_ID] ?: UUID.randomUUID().toString().also {
            context.dataStore.edit { p -> p[DEVICE_ID] = it }
        }
    }

    suspend fun savePatientId(id: String) {
        context.dataStore.edit { it[PATIENT_ID] = id }
    }

    suspend fun getMqttHost(): String =
        context.dataStore.data.first()[MQTT_HOST] ?: com.rpm.watch.BuildConfig.MQTT_HOST

    suspend fun getMqttPort(): Int =
        context.dataStore.data.first()[MQTT_PORT] ?: com.rpm.watch.BuildConfig.MQTT_PORT

    suspend fun saveMqttConfig(host: String, port: Int) {
        context.dataStore.edit {
            it[MQTT_HOST] = host
            it[MQTT_PORT] = port
        }
    }
}
