using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.DTOs.Devices;
using RPM.Application.Features.Devices;

namespace RPM.API.Controllers;

[ApiController]
[Route("api/devices")]
[Authorize]
public class DevicesController(IMediator mediator) : ControllerBase
{
    /// <summary>Returns the linked smartwatch devices for the current patient.</summary>
    [HttpGet]
    [Authorize(Roles = "Patient")]
    public async Task<IActionResult> GetMyDevices(CancellationToken ct) =>
        Ok(await mediator.Send(new GetMyDevicesQuery(), ct));

    /// <summary>
    /// Returns the Patient ID and MQTT broker settings the patient needs to
    /// configure their Galaxy Watch (shown as a QR code in the mobile app).
    /// </summary>
    [HttpGet("pairing-info")]
    [Authorize(Roles = "Patient")]
    public async Task<IActionResult> GetPairingInfo(CancellationToken ct) =>
        Ok(await mediator.Send(new GetPairingInfoQuery(), ct));

    /// <summary>Renames a device (patient-owned only).</summary>
    [HttpPatch("{id:guid}/name")]
    [Authorize(Roles = "Patient")]
    public async Task<IActionResult> RenameDevice(Guid id, [FromBody] RenameDeviceRequest body, CancellationToken ct)
    {
        await mediator.Send(new RenameDeviceCommand(id, body.NewName), ct);
        return NoContent();
    }
}
