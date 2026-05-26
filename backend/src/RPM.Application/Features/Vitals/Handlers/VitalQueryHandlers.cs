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
        return new VitalsPagedDto(items.Select(VitalMapper.ToDto), total, q.Page, q.PageSize);
    }
}

public class GetLatestVitalsHandler(IUnitOfWork uow, ICacheService cache)
    : IRequestHandler<GetLatestVitalsQuery, VitalRecordDto?>
{
    public async Task<VitalRecordDto?> Handle(GetLatestVitalsQuery q, CancellationToken ct)
    {
        var cached = await cache.GetAsync<VitalRecordDto>(VitalMapper.LatestVitalsKey(q.PatientId), ct);
        if (cached is not null) return cached;

        var record = await uow.Vitals.GetLatestByPatientIdAsync(q.PatientId, ct);
        if (record is null) return null;
        var dto = VitalMapper.ToDto(record);
        await cache.SetAsync(VitalMapper.LatestVitalsKey(q.PatientId), dto, TimeSpan.FromHours(6), ct);
        return dto;
    }
}

public class GetAlertThresholdHandler(IUnitOfWork uow)
    : IRequestHandler<GetAlertThresholdQuery, AlertThresholdDto?>
{
    public async Task<AlertThresholdDto?> Handle(GetAlertThresholdQuery q, CancellationToken ct)
    {
        var threshold = await uow.Alerts.GetThresholdByPatientIdAsync(q.PatientId, ct);
        if (threshold is null) return null;
        return new AlertThresholdDto(
            threshold.MinHeartRate,
            threshold.MaxHeartRate,
            threshold.MinSpO2,
            threshold.MaxSystolicBp,
            threshold.MaxDiastolicBp,
            threshold.MaxTemperatureC,
            threshold.MaxSkinTemperatureC,
            threshold.MinRespirationRate,
            threshold.MaxRespirationRate,
            threshold.MaxStressScore,
            threshold.MinBloodGlucoseMgDl,
            threshold.MaxBloodGlucoseMgDl);
    }
}
