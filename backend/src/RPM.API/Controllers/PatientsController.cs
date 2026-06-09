using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.DTOs.Patients;
using RPM.Application.Common.Interfaces;
using RPM.Domain.Entities;
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
        var result = new List<PatientSummaryDto>();
        foreach (var p in patients)
        {
            var latest = await uow.Vitals.GetLatestByPatientIdAsync(p.UserId, ct);
            result.Add(new PatientSummaryDto(
                p.UserId,
                p.Id,
                p.User.FullName,
                p.User.Email,
                p.User.Phone,
                p.User.AvatarUrl,
                p.DateOfBirth,
                p.BloodType?.ToString(),
                p.User.IsActive,
                latest is null ? null : ToVitalRecordLatestDto(latest)));
        }
        return Ok(result);
    }

    [HttpGet("{userId:guid}")]
    public async Task<IActionResult> GetPatient(Guid userId, CancellationToken ct)
    {
        var profile = await uow.Patients.GetByPatientUserIdAsync(userId, ct);
        if (profile is null) return NotFound();

        var latest = await uow.Vitals.GetLatestByPatientIdAsync(userId, ct);
        var doctorDtos = new List<DoctorAssignmentDto>();
        foreach (var assignment in profile.DoctorAssignments)
        {
            var doctorUser = await uow.Users.GetByIdAsync(assignment.DoctorId, ct);
            var doctorProfile = await uow.Patients.GetDoctorProfileByUserIdAsync(assignment.DoctorId, ct);
            if (doctorUser is null) continue;
            doctorDtos.Add(new DoctorAssignmentDto(
                doctorUser.Id,
                doctorUser.FullName,
                doctorProfile?.Specialization ?? string.Empty,
                assignment.Status.ToString(),
                assignment.AssignedAt));
        }

        var primaryDoctor = doctorDtos.FirstOrDefault(d => d.Status == "Active")
            ?? doctorDtos.FirstOrDefault();

        var dto = new PatientDetailDto(
            profile.UserId,
            profile.Id,
            profile.User.FullName,
            profile.User.Email,
            profile.User.Phone,
            profile.User.AvatarUrl,
            profile.DateOfBirth,
            profile.BloodType?.ToString(),
            profile.WeightKg,
            profile.HeightCm,
            profile.ChronicDiseases,
            profile.Allergies,
            profile.CurrentMedications,
            profile.EmergencyContactPhone,
            latest is null ? null : ToVitalRecordLatestDto(latest),
            doctorDtos,
            profile.WatchShortId,
            primaryDoctor is null ? null : new DoctorSimpleDto(
                primaryDoctor.DoctorUserId,
                primaryDoctor.DoctorName,
                primaryDoctor.Specialization));
        return Ok(dto);
    }

    /// <summary>Patient saves their watch short ID (6 chars). Both web and mobile use this to derive the streaming UUID.</summary>
    [HttpPut("{userId:guid}/watch-setup")]
    public async Task<IActionResult> SetWatchShortId(Guid userId, [FromBody] SetWatchShortIdRequest req, CancellationToken ct)
    {
        if (currentUser.UserId != userId && !User.IsInRole("Doctor") && !User.IsInRole("Admin"))
            return Forbid();

        var shortId = req.ShortId?.Trim().ToUpperInvariant();
        if (!string.IsNullOrEmpty(shortId) && (shortId.Length != 6 || !shortId.All(c => char.IsLetterOrDigit(c))))
            return BadRequest(new { error = "Watch short ID must be exactly 6 alphanumeric characters." });

        var profile = await uow.Patients.GetByUserIdAsync(userId, ct);
        if (profile is null) return NotFound();

        profile.SetWatchShortId(shortId);
        uow.Patients.Update(profile);
        await uow.SaveChangesAsync(ct);
        return Ok(new { watchShortId = profile.WatchShortId });
    }

    [HttpPost("{patientUserId:guid}/assign-doctor/{doctorUserId:guid}")]
    [Authorize(Roles = "Doctor,Admin")]
    public async Task<IActionResult> AssignDoctor(Guid patientUserId, Guid doctorUserId, CancellationToken ct)
    {
        var profile = await uow.Patients.GetByUserIdAsync(patientUserId, ct);
        if (profile is null) return NotFound("Patient not found.");

        var existing = await uow.Patients.GetAssignmentAsync(doctorUserId, profile.Id, ct);
        if (existing is not null)
        {
            existing.Activate();
            uow.Patients.UpdateAssignment(existing);
        }
        else
        {
            var assignment = Domain.Entities.DoctorPatientAssignment.Create(doctorUserId, profile.Id);
            assignment.Activate();
            await uow.Patients.AddAssignmentAsync(assignment, ct);
        }
        await uow.SaveChangesAsync(ct);
        return Ok(new { message = "Doctor assigned successfully" });
    }

    [HttpPost("{patientUserId:guid}/link-relative/{relativeUserId:guid}")]
    public async Task<IActionResult> LinkRelative(Guid patientUserId, Guid relativeUserId,
        [FromBody] string relationship, CancellationToken ct)
    {
        var profile = await uow.Patients.GetByUserIdAsync(patientUserId, ct);
        if (profile is null) return NotFound("Patient not found.");
        var link = Domain.Entities.PatientRelativeLink.Create(profile.Id, relativeUserId, relationship);
        await uow.Patients.AddRelativeLinkAsync(link, ct);
        await uow.SaveChangesAsync(ct);
        return Ok(new { message = "Relative linked successfully" });
    }

    private static VitalRecordLatestDto ToVitalRecordLatestDto(VitalRecord r) => new(
        r.HeartRateBpm,
        r.SpO2Percent,
        r.SystolicBp,
        r.DiastolicBp,
        r.TemperatureC,
        r.SkinTemperatureC,
        r.HeartRateVariabilityMs,
        r.StressScore,
        r.BodyFatPercent,
        r.EcgAverageHeartRate,
        r.StepsCount,
        r.CaloriesBurned,
        r.FallDetected,
        r.IsWearing,
        r.RecordedAt);
}
