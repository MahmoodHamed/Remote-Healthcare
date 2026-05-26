using MediatR;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Vitals;
using RPM.Application.Features.Vitals.Queries;
using RPM.Domain.Interfaces;
namespace RPM.Application.Features.Vitals.Handlers;

public class GetPatientVitalsHandler(IUnitOfWork uow)
    : IRequestHandler<GetPatientVitalsQuery, VitalsPagedDto>
{
    public async Task<VitalsPagedDto> Handle(GetPatientVitalsQuery q, CancellationToken ct)
    {
        var items = await uow.Vitals.GetByPatientIdAsync(q.PatientId, q.From, q.To, q.Page, q.PageSize, ct);
        var total = await uow.Vitals.CountByPatientIdAsync(q.PatientId, q.From, q.To, ct);
        var dtos = items.Select(r => new VitalRecordDto(r.Id, r.PatientId, r.DeviceId,
            r.HeartRateBpm, r.SpO2Percent, r.SystolicBp, r.DiastolicBp,
            r.TemperatureC, r.StepsCount, r.FallDetected, r.IsWearing, r.RecordedAt));
        return new VitalsPagedDto(dtos, total, q.Page, q.PageSize);
    }
}

public class GetLatestVitalsHandler(IUnitOfWork uow, ICacheService cache)
    : IRequestHandler<GetLatestVitalsQuery, VitalRecordDto?>
{
    public async Task<VitalRecordDto?> Handle(GetLatestVitalsQuery q, CancellationToken ct)
    {
        var cached = await cache.GetAsync<VitalRecordDto>(LatestVitalsKey(q.PatientId), ct);
        if (cached is not null) return cached;

        var r = await uow.Vitals.GetLatestByPatientIdAsync(q.PatientId, ct);
        if (r is null) return null;
        var dto = new VitalRecordDto(r.Id, r.PatientId, r.DeviceId, r.HeartRateBpm, r.SpO2Percent,
            r.SystolicBp, r.DiastolicBp, r.TemperatureC, r.StepsCount, r.FallDetected, r.IsWearing, r.RecordedAt);
        await cache.SetAsync(LatestVitalsKey(q.PatientId), dto, TimeSpan.FromHours(6), ct);
        return dto;
    }

    private static string LatestVitalsKey(Guid patientId) => $"patient:{patientId:D}:latest_vitals";
}

public class GetAlertThresholdHandler(IUnitOfWork uow)
    : IRequestHandler<GetAlertThresholdQuery, AlertThresholdDto?>
{
    public async Task<AlertThresholdDto?> Handle(GetAlertThresholdQuery q, CancellationToken ct)
    {
        var t = await uow.Alerts.GetThresholdByPatientIdAsync(q.PatientId, ct);
        if (t is null) return null;
        return new AlertThresholdDto(t.MinHeartRate, t.MaxHeartRate, t.MinSpO2, t.MaxSystolicBp, t.MaxDiastolicBp, t.MaxTemperatureC);
    }
}
