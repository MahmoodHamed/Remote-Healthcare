namespace RPM.Application.DTOs.Vitals;
public record VitalRecordDto(
    Guid Id, Guid PatientId, Guid DeviceId,
    float? HeartRateBpm, float? SpO2Percent,
    float? SystolicBp, float? DiastolicBp,
    float? TemperatureC, int? StepsCount,
    bool FallDetected, bool IsWearing,
    DateTime RecordedAt);

public record VitalsPagedDto(IEnumerable<VitalRecordDto> Items, long TotalCount, int Page, int PageSize);
public record AlertThresholdDto(float MinHeartRate, float MaxHeartRate, float MinSpO2,
    float MaxSystolicBp, float MaxDiastolicBp, float MaxTemperatureC);
