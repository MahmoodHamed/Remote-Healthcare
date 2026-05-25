# Watch to Server Connection

The watch app already publishes heart-rate telemetry to the RPM MQTT broker. Use this guide to connect a watch to the server and verify that the backend is receiving readings.

## Broker settings

- MQTT host: `remote-care.tech` (domain for this project — do not use the shared server IP)
- MQTT port: `1883` for local/dev deployments
- Topic: `vitals/{patientId}/data`

## Watch setup

1. Start the backend stack from `docker/docker-compose.yml`.
2. Open the watch app and enter the patient ID assigned to the patient.
3. Set the MQTT host to the server address if it is not already saved.
4. Tap Start on the watch.

The watch publishes a reading every 5 seconds while the heart-rate service is active.

## Payload sent to the server

```json
{
  "patientId": "00000000-0000-0000-0000-000000000001",
  "deviceId": "2d9f7f10-5d4b-4f8f-8d92-7a5b8c9f1e21",
  "heartRateBpm": 78,
  "spO2Percent": null,
  "systolicBp": null,
  "diastolicBp": null,
  "temperatureC": 36.6,
  "stepsCount": 124,
  "caloriesBurned": 4.9,
  "fallDetected": false,
  "isWearing": true
}
```

## Prompt to send to the server

Use this prompt when you want to verify the server-side ingestion path:

> Connect the watch to the RPM MQTT broker at `<server-host>:1883`. Publish heart-rate telemetry to `vitals/{patientId}/data` as JSON with `patientId`, `deviceId`, `heartRateBpm`, `spO2Percent`, `systolicBp`, `diastolicBp`, `temperatureC`, `stepsCount`, `caloriesBurned`, `fallDetected`, and `isWearing`. Keep `patientId` and `deviceId` as stable identifiers so the backend can map readings to the correct patient and device.

## Backend fallback

If you need a direct HTTP test instead of MQTT, the API exposes a fallback ingest endpoint at `POST /api/patients/{patientId}/vitals`.