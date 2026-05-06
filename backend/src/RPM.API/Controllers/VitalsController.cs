using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.Features.Vitals.Commands;
using RPM.Application.Features.Vitals.Queries;
namespace RPM.API.Controllers;

[ApiController]
[Route("api/patients/{patientId}/vitals")]
[Authorize]
public class VitalsController(IMediator mediator) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetVitals(Guid patientId,
        [FromQuery] DateTime from, [FromQuery] DateTime to,
        [FromQuery] int page = 1, [FromQuery] int pageSize = 50,
        CancellationToken ct = default) =>
        Ok(await mediator.Send(new GetPatientVitalsQuery(patientId, from, to, page, pageSize), ct));

    [HttpGet("latest")]
    public async Task<IActionResult> GetLatest(Guid patientId, CancellationToken ct) =>
        Ok(await mediator.Send(new GetLatestVitalsQuery(patientId), ct));

    [HttpGet("threshold")]
    public async Task<IActionResult> GetThreshold(Guid patientId, CancellationToken ct) =>
        Ok(await mediator.Send(new GetAlertThresholdQuery(patientId), ct));

    [HttpPut("threshold")]
    [Authorize(Roles = "Doctor")]
    public async Task<IActionResult> UpdateThreshold(Guid patientId,
        [FromBody] UpdateAlertThresholdCommand cmd, CancellationToken ct)
    {
        await mediator.Send(cmd with { PatientId = patientId }, ct);
        return NoContent();
    }

    /// <summary>Direct REST ingestion (wearable fallback - prefer MQTT)</summary>
    [HttpPost]
    public async Task<IActionResult> IngestVital(Guid patientId,
        [FromBody] IngestVitalCommand cmd, CancellationToken ct) =>
        Ok(await mediator.Send(cmd with { PatientId = patientId }, ct));
}
