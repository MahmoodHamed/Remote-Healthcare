using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.DTOs.Patients;
using RPM.Application.Common.Interfaces;
using RPM.Domain.Interfaces;
namespace RPM.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class PatientsController(IUnitOfWork uow, ICurrentUser currentUser) : ControllerBase
{
    [HttpGet]
    [Authorize(Roles = "Doctor")]
    public async Task<IActionResult> GetMyPatients(CancellationToken ct)
    {
        var patients = await uow.Patients.GetByDoctorIdAsync(currentUser.UserId, ct);
        var result = patients.Select(p => new PatientSummaryDto(
            p.UserId, p.User.FullName, p.User.Email, p.User.AvatarUrl,
            p.DateOfBirth, p.BloodType?.ToString()));
        return Ok(result);
    }

    [HttpGet("{userId}")]
    public async Task<IActionResult> GetPatient(Guid userId, CancellationToken ct)
    {
        var profile = await uow.Patients.GetByUserIdAsync(userId, ct);
        if (profile is null) return NotFound();

        var latest = await uow.Vitals.GetLatestByPatientIdAsync(userId, ct);
        var dto = new PatientDetailDto(
            profile.UserId, profile.User.FullName, profile.User.Email, profile.User.Phone,
            profile.User.AvatarUrl, profile.DateOfBirth, profile.BloodType?.ToString(),
            profile.WeightKg, profile.HeightCm, profile.ChronicDiseases,
            profile.Allergies, profile.CurrentMedications, profile.EmergencyContactPhone,
            latest is null ? null : new VitalRecordLatestDto(
                latest.HeartRateBpm, latest.SpO2Percent, latest.SystolicBp,
                latest.DiastolicBp, latest.TemperatureC, latest.RecordedAt));
        return Ok(dto);
    }

    [HttpPost("{patientId}/assign-doctor/{doctorId}")]
    [Authorize(Roles = "Doctor")]
    public async Task<IActionResult> AssignDoctor(Guid patientId, Guid doctorId, CancellationToken ct)
    {
        var assignment = RPM.Domain.Entities.DoctorPatientAssignment.Create(doctorId, patientId);
        assignment.Activate();
        await uow.Patients.AddAssignmentAsync(assignment, ct);
        await uow.SaveChangesAsync(ct);
        return Ok(new { message = "Doctor assigned successfully" });
    }

    [HttpPost("{patientId}/link-relative/{relativeId}")]
    public async Task<IActionResult> LinkRelative(Guid patientId, Guid relativeId,
        [FromBody] string relationship, CancellationToken ct)
    {
        var link = RPM.Domain.Entities.PatientRelativeLink.Create(patientId, relativeId, relationship);
        await uow.Patients.AddRelativeLinkAsync(link, ct);
        await uow.SaveChangesAsync(ct);
        return Ok(new { message = "Relative linked successfully" });
    }
}
