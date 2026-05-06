package com.rpm.watch.mqtt

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "MqttManager"

enum class MqttConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

@Singleton
class MqttManager @Inject constructor() {

    private val _connectionState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState

    private var client: Mqtt3AsyncClient? = null

    /** Build and connect the HiveMQ async client. */
    suspend fun connect(host: String, port: Int, clientId: String) {
        if (_connectionState.value == MqttConnectionState.CONNECTED) return
        _connectionState.value = MqttConnectionState.CONNECTING

        client = MqttClient.builder()
            .useMqttVersion3()
            .serverHost(host)
            .serverPort(port)
            .identifier(clientId)
            .automaticReconnectWithDefaultConfig()
            .buildAsync()

        suspendCancellableCoroutine { cont ->
            client!!.connectWith()
                .cleanSession(false)
                .keepAlive(60)
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "MQTT connect failed: ${throwable.message}")
                        _connectionState.value = MqttConnectionState.ERROR
                        if (cont.isActive) cont.resumeWithException(throwable)
                    } else {
                        Log.i(TAG, "MQTT connected to $host:$port (id=$clientId)")
                        _connectionState.value = MqttConnectionState.CONNECTED
                        if (cont.isActive) cont.resume(Unit)
                    }
                }

            cont.invokeOnCancellation { disconnect() }
        }
    }

    /**
     * Publishes [payload] (UTF-8) to [topic] with QoS-1 (at-least-once).
     * Returns silently if not connected.
     */
    fun publish(topic: String, payload: String) {
        val c = client ?: return
        if (_connectionState.value != MqttConnectionState.CONNECTED) {
            Log.w(TAG, "Publish skipped — not connected")
            return
        }
        c.publishWith()
            .topic(topic)
            .payload(payload.toByteArray(StandardCharsets.UTF_8))
            .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
            .retain(false)
            .send()
            .whenComplete { _: Mqtt3Publish?, error: Throwable? ->
                if (error != null) {
                    Log.e(TAG, "Publish error on $topic: ${error.message}")
                } else {
                    Log.d(TAG, "Published to $topic")
                }
            }
    }

    fun disconnect() {
        try {
            client?.disconnect()
        } catch (_: Exception) {}
        _connectionState.value = MqttConnectionState.DISCONNECTED
        client = null
    }
}
