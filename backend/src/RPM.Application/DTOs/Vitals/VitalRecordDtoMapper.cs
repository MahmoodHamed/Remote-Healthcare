using RPM.Domain.Entities;

namespace RPM.Application.DTOs.Vitals;

internal static class VitalRecordDtoMapper
{
    internal static VitalRecordDto MapToDto(VitalRecord r) =>
        new(r.Id, r.PatientId, r.DeviceId, r.HeartRateBpm, r.SpO2Percent,
            r.SystolicBp, r.DiastolicBp, r.TemperatureC, r.SkinTemperatureC, r.AmbientTemperatureC,
            r.HrvMs, r.StressScore, r.BodyFatPercent, r.EcgAvgHeartRateBpm,
            r.StepsCount, r.CaloriesBurned, r.FallDetected, r.IsWearing, r.RecordedAt);
}
