using MediatR;
using RPM.Application.Common.Exceptions;
using RPM.Application.DTOs.Alerts;
using RPM.Application.Features.Alerts.Commands;
using RPM.Application.Features.Alerts.Queries;
using RPM.Domain.Entities;
using RPM.Domain.Interfaces;
namespace RPM.Application.Features.Alerts.Handlers;

public class ResolveAlertCommandHandler(IUnitOfWork uow)
    : IRequestHandler<ResolveAlertCommand, AlertDto>
{
    public async Task<AlertDto> Handle(ResolveAlertCommand cmd, CancellationToken ct)
    {
        var alert = await uow.Alerts.GetByIdAsync(cmd.AlertId, ct)
            ?? throw new NotFoundException(nameof(Alert), cmd.AlertId);
        alert.Resolve(cmd.ResolvedById);
        uow.Alerts.Update(alert);
        await uow.SaveChangesAsync(ct);
        return AlertMapper.Map(alert);
    }
}

public class DismissAlertCommandHandler(IUnitOfWork uow)
    : IRequestHandler<DismissAlertCommand>
{
    public async Task Handle(DismissAlertCommand cmd, CancellationToken ct)
    {
        var alert = await uow.Alerts.GetByIdAsync(cmd.AlertId, ct)
            ?? throw new NotFoundException(nameof(Alert), cmd.AlertId);
        alert.Dismiss();
        uow.Alerts.Update(alert);
        await uow.SaveChangesAsync(ct);
    }
}

public class GetPatientAlertsHandler(IUnitOfWork uow)
    : IRequestHandler<GetPatientAlertsQuery, AlertPagedDto>
{
    public async Task<AlertPagedDto> Handle(GetPatientAlertsQuery q, CancellationToken ct)
    {
        var items = await uow.Alerts.GetByPatientIdAsync(q.PatientId, q.Page, q.PageSize, ct);
        return new AlertPagedDto(items.Select(AlertMapper.Map), items.Count(), q.Page, q.PageSize);
    }
}

public class GetUnresolvedAlertsHandler(IUnitOfWork uow)
    : IRequestHandler<GetUnresolvedAlertsQuery, IEnumerable<AlertDto>>
{
    public async Task<IEnumerable<AlertDto>> Handle(GetUnresolvedAlertsQuery q, CancellationToken ct)
    {
        var items = await uow.Alerts.GetUnresolvedByPatientIdAsync(q.PatientId, ct);
        return items.Select(AlertMapper.Map);
    }
}

file static class AlertMapper
{
    public static AlertDto Map(Alert a) =>
        new(a.Id, a.PatientId, a.Type.ToString(), a.Severity.ToString(), a.Message, a.Status.ToString(), a.TriggeredAt, a.ResolvedAt);
}
