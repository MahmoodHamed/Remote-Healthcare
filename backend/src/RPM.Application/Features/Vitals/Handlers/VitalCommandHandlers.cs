using MediatR;
using RPM.Application.Common.Exceptions;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Vitals;
using RPM.Application.Features.Vitals.Commands;
using RPM.Domain.Entities;
using RPM.Domain.Enums;
using RPM.Domain.Interfaces;
namespace RPM.Application.Features.Vitals.Handlers;

public class IngestVitalCommandHandler(IUnitOfWork uow, IVitalsHubService hub)
    : IRequestHandler<IngestVitalCommand, VitalRecordDto>
{
    public async Task<VitalRecordDto> Handle(IngestVitalCommand cmd, CancellationToken ct)
    {
        var record = VitalRecord.Create(cmd.PatientId, cmd.DeviceId,
            cmd.HeartRateBpm, cmd.SpO2Percent, cmd.SystolicBp, cmd.DiastolicBp,
            cmd.TemperatureC, cmd.Steps, cmd.Calories, cmd.FallDetected, cmd.IsWearing);

        await uow.Vitals.AddAsync(record, ct);
        await uow.SaveChangesAsync(ct);

        var dto = MapToDto(record);
        // Broadcast real-time
        await hub.BroadcastVitalsAsync(cmd.PatientId, dto, ct);

        return dto;
    }

    private static VitalRecordDto MapToDto(VitalRecord r) =>
        new(r.Id, r.PatientId, r.DeviceId, r.HeartRateBpm, r.SpO2Percent,
            r.SystolicBp, r.DiastolicBp, r.TemperatureC, r.StepsCount,
            r.FallDetected, r.IsWearing, r.RecordedAt);
}

public class UpdateAlertThresholdHandler(IUnitOfWork uow)
    : IRequestHandler<UpdateAlertThresholdCommand>
{
    public async Task Handle(UpdateAlertThresholdCommand cmd, CancellationToken ct)
    {
        var threshold = await uow.Alerts.GetThresholdByPatientIdAsync(cmd.PatientId, ct);
        if (threshold is null)
        {
            var t = AlertThreshold.CreateDefault(cmd.PatientId);
            t.Update(cmd.MinHeartRate, cmd.MaxHeartRate, cmd.MinSpO2, cmd.MaxSystolicBp, cmd.MaxDiastolicBp, cmd.MaxTemperatureC);
            await uow.Alerts.AddThresholdAsync(t, ct);
        }
        else
        {
            threshold.Update(cmd.MinHeartRate, cmd.MaxHeartRate, cmd.MinSpO2, cmd.MaxSystolicBp, cmd.MaxDiastolicBp, cmd.MaxTemperatureC);
            uow.Alerts.UpdateThreshold(threshold);
        }
        await uow.SaveChangesAsync(ct);
    }
}
