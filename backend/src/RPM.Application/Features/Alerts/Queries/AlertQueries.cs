using MediatR;
using RPM.Application.DTOs.Alerts;
namespace RPM.Application.Features.Alerts.Queries;
public record GetPatientAlertsQuery(Guid PatientId, int Page = 1, int PageSize = 20) : IRequest<AlertPagedDto>;
public record GetUnresolvedAlertsQuery(Guid PatientId) : IRequest<IEnumerable<AlertDto>>;
