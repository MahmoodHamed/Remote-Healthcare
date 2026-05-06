using MediatR;
using RPM.Application.DTOs.Vitals;
namespace RPM.Application.Features.Vitals.Commands;

public record IngestVitalCommand(
    Guid PatientId, Guid DeviceId,
    float? HeartRateBpm, float? SpO2Percent,
    float? SystolicBp, float? DiastolicBp,
    float? TemperatureC, int? Steps,
    float? Calories, bool FallDetected, bool IsWearing) : IRequest<VitalRecordDto>;

public record UpdateAlertThresholdCommand(
    Guid PatientId, float MinHeartRate, float MaxHeartRate,
    float MinSpO2, float MaxSystolicBp, float MaxDiastolicBp, float MaxTemperatureC) : IRequest;
