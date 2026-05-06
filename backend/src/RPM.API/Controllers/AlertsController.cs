using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.Features.Alerts.Commands;
using RPM.Application.Features.Alerts.Queries;
namespace RPM.API.Controllers;

[ApiController]
[Route("api/patients/{patientId}/alerts")]
[Authorize]
public class AlertsController(IMediator mediator) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetAlerts(Guid patientId,
        [FromQuery] int page = 1, [FromQuery] int pageSize = 20, CancellationToken ct = default) =>
        Ok(await mediator.Send(new GetPatientAlertsQuery(patientId, page, pageSize), ct));

    [HttpGet("unresolved")]
    public async Task<IActionResult> GetUnresolved(Guid patientId, CancellationToken ct) =>
        Ok(await mediator.Send(new GetUnresolvedAlertsQuery(patientId), ct));

    [HttpPost("{alertId}/resolve")]
    [Authorize(Roles = "Doctor")]
    public async Task<IActionResult> Resolve(Guid patientId, Guid alertId,
        [FromBody] ResolveAlertCommand cmd, CancellationToken ct) =>
        Ok(await mediator.Send(cmd with { AlertId = alertId }, ct));

    [HttpPost("{alertId}/dismiss")]
    public async Task<IActionResult> Dismiss(Guid patientId, Guid alertId, CancellationToken ct)
    {
        await mediator.Send(new DismissAlertCommand(alertId), ct);
        return NoContent();
    }
}
