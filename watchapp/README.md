# Galaxy Watch 8 – Vitals App

Wear OS app for **Samsung Galaxy Watch 8** (Samsung Health Sensor SDK). Streams heart rate, skin temperature, and SpO₂ to the RPM MQTT broker.

## Sensor modules (`app/src/main/kotlin/com/rpm/watch/sensor/`)

| Path | Role |
|------|------|
| `SensorType.kt` | Active sensor: `HEART_RATE`, `SKIN_TEMPERATURE`, `SPO2` |
| `VitalsModels.kt` | Shared readings & tracker state |
| `VitalsSensorCoordinator.kt` | Samsung SDK session + routes to parsers |
| `heart/HeartRateSamsungParser.kt` | Samsung HR `DataPoint` parsing |
| `temperature/SkinTemperatureSamsungParser.kt` | Samsung skin temp parsing |
| `spo2/SpO2SamsungParser.kt` | Samsung SpO₂ parsing |
| `platform/PlatformHeartRateReader.kt` | Android SensorManager HR fallback |
| `platform/PlatformSkinTemperatureReader.kt` | Platform skin temp fallback |
| `platform/PlatformSpO2Reader.kt` | Platform SpO₂ fallback |
| `platform/PlatformSensorHub.kt` | Starts the active platform reader |
| `motion/MotionSensorHub.kt` | Steps & fall (accelerometer) |
| `samsung/SamsungTrackerResolver.kt` | Maps `SensorType` → `HealthTrackerType` |

## Service & UI

- `service/VitalsMonitorService.kt` – foreground service, MQTT publish
- `ui/VitalsMonitorScreen.kt` – scrollable vitals UI

## Setup

1. Place `samsung-health-sensor-api.aar` in `app/libs/`.
2. Set patient ID and MQTT host in the watch app.
3. Tap **Start** and grant **Body sensors** (and skin temperature if prompted).

## MQTT topic

`vitals/{patientId}/data` — JSON: `heartRateBpm`, `temperatureC`, `spO2Percent`, `stepsCount`, `fallDetected`, etc.
