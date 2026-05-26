using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.DTOs.Patients;
using RPM.Application.Features.Admin;

namespace RPM.API.Controllers;

/// <summary>
/// Hospital-wide admin directory: lets the administrator see every doctor and
/// every patient on the platform, drill into a doctor's patient list, and manage
/// doctor-patient assignments.
/// </summary>
[ApiController]
[Route("api/admin")]
[Authorize(Roles = "Admin")]
public class AdminDirectoryController(IMediator mediator) : ControllerBase
{
    [HttpGet("overview")]
    public async Task<IActionResult> Overview(CancellationToken ct) =>
        Ok(await mediator.Send(new GetAdminOverviewQuery(), ct));

    [HttpGet("doctors")]
    public async Task<IActionResult> Doctors(CancellationToken ct) =>
        Ok(await mediator.Send(new GetAllDoctorsQuery(), ct));

    [HttpGet("doctors/{doctorUserId:guid}")]
    public async Task<IActionResult> DoctorDetail(Guid doctorUserId, CancellationToken ct) =>
        Ok(await mediator.Send(new GetDoctorWithPatientsQuery(doctorUserId), ct));

    [HttpGet("patients")]
    public async Task<IActionResult> Patients(CancellationToken ct) =>
        Ok(await mediator.Send(new GetAllPatientsForAdminQuery(), ct));

    [HttpPost("assignments")]
    public async Task<IActionResult> Assign([FromBody] AssignmentRequestDto request, CancellationToken ct)
    {
        await mediator.Send(new AssignDoctorToPatientCommand(request.DoctorUserId, request.PatientUserId), ct);
        return NoContent();
    }

    [HttpDelete("assignments")]
    public async Task<IActionResult> Revoke([FromBody] AssignmentRequestDto request, CancellationToken ct)
    {
        await mediator.Send(new RevokeDoctorAssignmentCommand(request.DoctorUserId, request.PatientUserId), ct);
        return NoContent();
    }
}
