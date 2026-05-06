using MediatR;
using RPM.Application.DTOs.Alerts;
namespace RPM.Application.Features.Alerts.Commands;
public record ResolveAlertCommand(Guid AlertId, Guid ResolvedById) : IRequest<AlertDto>;
public record DismissAlertCommand(Guid AlertId) : IRequest;
