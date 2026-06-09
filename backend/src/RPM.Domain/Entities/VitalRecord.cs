using RPM.Domain.Common;
using RPM.Domain.Events;
namespace RPM.Domain.Entities;

/// <summary>
/// A single vitals reading from a wearable device. All fields are optional so the
/// record can be produced from continuous, on-demand, or partial sensor sources
/// (Samsung Watch 8 mixes continuous HR/accel with on-demand SpO2/ECG/BIA).
/// </summary>
public class VitalRecord : BaseEntity
{
    public Guid PatientId { get; private set; }
    public User Patient { get; private set; } = null!;
    public Guid DeviceId { get; private set; }
    public Device Device { get; private set; } = null!;

    // Cardio
    public float? HeartRateBpm { get; private set; }
    public float? HeartRateVariabilityMs { get; private set; }
    public float? RestingHeartRateBpm { get; private set; }
    public float? MaxHeartRateBpm { get; private set; }

    // Respiratory
    public float? SpO2Percent { get; private set; }
    public float? RespirationRateBpm { get; private set; }

    // Blood pressure
    public float? SystolicBp { get; private set; }
    public float? DiastolicBp { get; private set; }

    // Temperature
    public float? TemperatureC { get; private set; }
    public float? SkinTemperatureC { get; private set; }

    // Activity & energy
    public int? StepsCount { get; private set; }
    public float? CaloriesBurned { get; private set; }
    public float? DistanceMeters { get; private set; }
    public int? FloorsClimbed { get; private set; }
    public int? ActiveMinutes { get; private set; }

    // Sleep & stress
    public float? StressScore { get; private set; }
    public float? SleepScore { get; private set; }
    public int? SleepDurationMinutes { get; private set; }

    // Body composition (BIA)
    public float? BodyFatPercent { get; private set; }
    public float? MuscleMassKg { get; private set; }
    public float? BodyWaterPercent { get; private set; }
    public float? BasalMetabolicRate { get; private set; }

    // ECG (summary; raw waveform stored as JSON if present)
    public float? EcgAverageHeartRate { get; private set; }
    public string? EcgClassification { get; private set; }
    public string? EcgWaveformJson { get; private set; }

    // Glucose (premium watch tier when available)
    public float? BloodGlucoseMgDl { get; private set; }

    // Safety & wear status
    public bool FallDetected { get; private set; }
    public bool IsWearing { get; private set; } = true;
    public float? BatteryLevel { get; private set; }

    public DateTime RecordedAt { get; private set; } = DateTime.UtcNow;

    public ICollection<Alert> Alerts { get; private set; } = [];

    protected VitalRecord() { }

    public static VitalRecord Create(
        Guid patientId,
        Guid deviceId,
        float? heartRate,
        float? spO2,
        float? systolicBp,
        float? diastolicBp,
        float? temperatureC,
        int? steps,
        float? calories,
        bool fallDetected,
        bool isWearing,
        float? skinTemperatureC = null,
        float? heartRateVariabilityMs = null,
        float? restingHeartRateBpm = null,
        float? maxHeartRateBpm = null,
        float? respirationRateBpm = null,
        float? distanceMeters = null,
        int? floorsClimbed = null,
        int? activeMinutes = null,
        float? stressScore = null,
        float? sleepScore = null,
        int? sleepDurationMinutes = null,
        float? bodyFatPercent = null,
        float? muscleMassKg = null,
        float? bodyWaterPercent = null,
        float? basalMetabolicRate = null,
        float? ecgAverageHeartRate = null,
        string? ecgClassification = null,
        string? ecgWaveformJson = null,
        float? bloodGlucoseMgDl = null,
        float? batteryLevel = null)
    {
        var record = new VitalRecord
        {
            PatientId = patientId,
            DeviceId = deviceId,
            HeartRateBpm = heartRate,
            SpO2Percent = spO2,
            SystolicBp = systolicBp,
            DiastolicBp = diastolicBp,
            TemperatureC = temperatureC,
            StepsCount = steps,
            CaloriesBurned = calories,
            FallDetected = fallDetected,
            IsWearing = isWearing,
            SkinTemperatureC = skinTemperatureC,
            HeartRateVariabilityMs = heartRateVariabilityMs,
            RestingHeartRateBpm = restingHeartRateBpm,
            MaxHeartRateBpm = maxHeartRateBpm,
            RespirationRateBpm = respirationRateBpm,
            DistanceMeters = distanceMeters,
            FloorsClimbed = floorsClimbed,
            ActiveMinutes = activeMinutes,
            StressScore = stressScore,
            SleepScore = sleepScore,
            SleepDurationMinutes = sleepDurationMinutes,
            BodyFatPercent = bodyFatPercent,
            MuscleMassKg = muscleMassKg,
            BodyWaterPercent = bodyWaterPercent,
            BasalMetabolicRate = basalMetabolicRate,
            EcgAverageHeartRate = ecgAverageHeartRate,
            EcgClassification = ecgClassification,
            EcgWaveformJson = ecgWaveformJson,
            BloodGlucoseMgDl = bloodGlucoseMgDl,
            BatteryLevel = batteryLevel,
            RecordedAt = DateTime.UtcNow
        };
        record.AddDomainEvent(new VitalRecordedEvent(record.Id, patientId, deviceId));
        return record;
    }
}
