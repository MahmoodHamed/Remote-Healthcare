using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.API.Helpers;
using RPM.Application.Features.Vitals.Commands;
using RPM.Application.Features.Vitals.Queries;
namespace RPM.API.Controllers;

[ApiController]
[Route("api/patients/{patientId}/vitals")]
[Authorize]
public class VitalsController(IMediator mediator) : ControllerBase
{
    private static Guid? ParsePatientId(string patientId) => PatientIdNormalizer.ToGuid(patientId);

    [HttpGet]
    public async Task<IActionResult> GetVitals(string patientId,
        [FromQuery] DateTime from, [FromQuery] DateTime to,
        [FromQuery] int page = 1, [FromQuery] int pageSize = 50,
        CancellationToken ct = default)
    {
        var id = ParsePatientId(patientId);
        if (id is null) return BadRequest("Invalid patient ID.");
        return Ok(await mediator.Send(new GetPatientVitalsQuery(id.Value, from, to, page, pageSize), ct));
    }

    [HttpGet("latest")]
    public async Task<IActionResult> GetLatest(string patientId, CancellationToken ct)
    {
        var id = ParsePatientId(patientId);
        if (id is null) return BadRequest("Invalid patient ID.");
        var latest = await mediator.Send(new GetLatestVitalsQuery(id.Value), ct);
        if (latest is null) return NoContent();
        return Ok(latest);
    }

    [HttpGet("threshold")]
    public async Task<IActionResult> GetThreshold(string patientId, CancellationToken ct)
    {
        var id = ParsePatientId(patientId);
        if (id is null) return BadRequest("Invalid patient ID.");
        return Ok(await mediator.Send(new GetAlertThresholdQuery(id.Value), ct));
    }

    [HttpPut("threshold")]
    [Authorize(Roles = "Doctor")]
    public async Task<IActionResult> UpdateThreshold(string patientId,
        [FromBody] UpdateAlertThresholdCommand cmd, CancellationToken ct)
    {
        var id = ParsePatientId(patientId);
        if (id is null) return BadRequest("Invalid patient ID.");
        await mediator.Send(cmd with { PatientId = id.Value }, ct);
        return NoContent();
    }

    /// <summary>Direct REST ingestion (wearable fallback - prefer MQTT)</summary>
    [HttpPost]
    public async Task<IActionResult> IngestVital(string patientId,
        [FromBody] IngestVitalCommand cmd, CancellationToken ct)
    {
        var id = ParsePatientId(patientId);
        if (id is null) return BadRequest("Invalid patient ID.");
        return Ok(await mediator.Send(cmd with { PatientId = id.Value }, ct));
    }
}
