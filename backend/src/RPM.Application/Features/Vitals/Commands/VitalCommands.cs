using MediatR;
using RPM.Application.DTOs.Vitals;
namespace RPM.Application.Features.Vitals.Commands;

public record IngestVitalCommand(
    Guid PatientId,
    Guid DeviceId,
    float? HeartRateBpm,
    float? SpO2Percent,
    float? SystolicBp,
    float? DiastolicBp,
    float? TemperatureC,
    int? Steps,
    float? Calories,
    bool FallDetected,
    bool IsWearing,
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
    float? BatteryLevel = null) : IRequest<VitalRecordDto>;

public record IngestVitalsBatchCommand(IReadOnlyCollection<VitalIngestionDto> Readings) : IRequest<int>;

public record UpdateAlertThresholdCommand(
    Guid PatientId,
    float MinHeartRate,
    float MaxHeartRate,
    float MinSpO2,
    float MaxSystolicBp,
    float MaxDiastolicBp,
    float MaxTemperatureC,
    float MaxSkinTemperatureC = 38.0f,
    float MinRespirationRate = 8,
    float MaxRespirationRate = 24,
    float MaxStressScore = 80,
    float MinBloodGlucoseMgDl = 70,
    float MaxBloodGlucoseMgDl = 180) : IRequest;
