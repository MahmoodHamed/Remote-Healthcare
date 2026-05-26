namespace RPM.Infrastructure.BackgroundServices;

/// <summary>
/// JSON contract that maps directly to <c>VitalsPayload.kt</c> on the watch.
/// All fields are optional so the watch can publish partial readings from
/// continuous sensors and on-demand sensors (SpO2, ECG, BIA) at different times.
/// </summary>
public record MqttVitalsPayload(
    string? PatientId,
    string? DeviceId,
    float? HeartRateBpm,
    float? SpO2Percent,
    float? SystolicBp,
    float? DiastolicBp,
    float? TemperatureC,
    int? StepsCount,
    float? CaloriesBurned,
    bool FallDetected,
    bool IsWearing,
    // Watch 8 advanced sensors
    float? SkinTemperatureC = null,
    float? HeartRateVariabilityMs = null,
    float? RestingHeartRateBpm = null,
    float? MaxHeartRateBpm = null,
    float? RespirationRateBpm = null,
    float? DistanceMeters = null,
    int? FloorsClimbed = null,
    int? ActiveMinutes = null,
    float? StressScore = null,
    float? SleepScore = null,
    int? SleepDurationMinutes = null,
    float? BodyFatPercent = null,
    float? MuscleMassKg = null,
    float? BodyWaterPercent = null,
    float? BasalMetabolicRate = null,
    float? EcgAverageHeartRate = null,
    string? EcgClassification = null,
    string? EcgWaveformJson = null,
    float? BloodGlucoseMgDl = null,
    float? BatteryLevel = null);
