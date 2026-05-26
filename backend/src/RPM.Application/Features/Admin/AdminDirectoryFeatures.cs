using MediatR;
using RPM.Application.Common.Exceptions;
using RPM.Application.DTOs.Patients;
using RPM.Domain.Entities;
using RPM.Domain.Enums;
using RPM.Domain.Interfaces;

namespace RPM.Application.Features.Admin;

public record GetAllDoctorsQuery() : IRequest<IReadOnlyList<DoctorDto>>;
public record GetDoctorWithPatientsQuery(Guid DoctorUserId) : IRequest<DoctorWithPatientsDto>;
public record GetAllPatientsForAdminQuery() : IRequest<IReadOnlyList<PatientSummaryDto>>;
public record AssignDoctorToPatientCommand(Guid DoctorUserId, Guid PatientUserId) : IRequest;
public record RevokeDoctorAssignmentCommand(Guid DoctorUserId, Guid PatientUserId) : IRequest;
public record GetAdminOverviewQuery() : IRequest<AdminOverviewDto>;

public class GetAllDoctorsQueryHandler(IUnitOfWork uow) : IRequestHandler<GetAllDoctorsQuery, IReadOnlyList<DoctorDto>>
{
    public async Task<IReadOnlyList<DoctorDto>> Handle(GetAllDoctorsQuery _, CancellationToken ct)
    {
        var doctors = await uow.Patients.GetAllDoctorsAsync(ct);
        return doctors.Select(d => new DoctorDto(
            d.UserId,
            d.User.FullName,
            d.User.Email,
            d.User.Phone,
            d.Specialization,
            d.LicenseNumber,
            d.HospitalName,
            d.User.AvatarUrl,
            d.User.IsActive,
            d.PatientAssignments.Count(a => a.Status == RelationshipAssignmentStatus.Active)
        )).ToList();
    }
}

public class GetDoctorWithPatientsQueryHandler(IUnitOfWork uow) : IRequestHandler<GetDoctorWithPatientsQuery, DoctorWithPatientsDto>
{
    public async Task<DoctorWithPatientsDto> Handle(GetDoctorWithPatientsQuery q, CancellationToken ct)
    {
        var doctorProfile = await uow.Patients.GetDoctorProfileByUserIdAsync(q.DoctorUserId, ct)
            ?? throw new NotFoundException("Doctor", q.DoctorUserId);
        var doctorUser = await uow.Users.GetByIdAsync(q.DoctorUserId, ct)
            ?? throw new NotFoundException("User", q.DoctorUserId);

        var patientProfiles = await uow.Patients.GetByDoctorIdAsync(q.DoctorUserId, ct);
        var patients = patientProfiles.Select(p => new PatientSummaryDto(
            p.UserId,
            p.Id,
            p.User.FullName,
            p.User.Email,
            p.User.Phone,
            p.User.AvatarUrl,
            p.DateOfBirth,
            p.BloodType?.ToString(),
            p.User.IsActive
        )).ToList();

        var dto = new DoctorDto(
            doctorUser.Id,
            doctorUser.FullName,
            doctorUser.Email,
            doctorUser.Phone,
            doctorProfile.Specialization,
            doctorProfile.LicenseNumber,
            doctorProfile.HospitalName,
            doctorUser.AvatarUrl,
            doctorUser.IsActive,
            patients.Count);

        return new DoctorWithPatientsDto(dto, patients);
    }
}

public class GetAllPatientsForAdminQueryHandler(IUnitOfWork uow) : IRequestHandler<GetAllPatientsForAdminQuery, IReadOnlyList<PatientSummaryDto>>
{
    public async Task<IReadOnlyList<PatientSummaryDto>> Handle(GetAllPatientsForAdminQuery _, CancellationToken ct)
    {
        var patients = await uow.Patients.GetAllPatientsAsync(ct);
        return patients.Select(p => new PatientSummaryDto(
            p.UserId,
            p.Id,
            p.User.FullName,
            p.User.Email,
            p.User.Phone,
            p.User.AvatarUrl,
            p.DateOfBirth,
            p.BloodType?.ToString(),
            p.User.IsActive
        )).ToList();
    }
}

public class AssignDoctorToPatientCommandHandler(IUnitOfWork uow) : IRequestHandler<AssignDoctorToPatientCommand>
{
    public async Task Handle(AssignDoctorToPatientCommand cmd, CancellationToken ct)
    {
        var doctor = await uow.Users.GetByIdAsync(cmd.DoctorUserId, ct)
            ?? throw new NotFoundException("Doctor", cmd.DoctorUserId);
        if (doctor.Role != UserRole.Doctor)
            throw new ConflictException("Target user is not a doctor.");

        var patientProfile = await uow.Patients.GetByUserIdAsync(cmd.PatientUserId, ct)
            ?? throw new NotFoundException("Patient", cmd.PatientUserId);

        var existing = await uow.Patients.GetAssignmentAsync(cmd.DoctorUserId, patientProfile.Id, ct);
        if (existing is not null)
        {
            existing.Activate();
            uow.Patients.UpdateAssignment(existing);
        }
        else
        {
            var assignment = DoctorPatientAssignment.Create(cmd.DoctorUserId, patientProfile.Id);
            assignment.Activate();
            await uow.Patients.AddAssignmentAsync(assignment, ct);
        }

        await uow.SaveChangesAsync(ct);
    }
}

public class RevokeDoctorAssignmentCommandHandler(IUnitOfWork uow) : IRequestHandler<RevokeDoctorAssignmentCommand>
{
    public async Task Handle(RevokeDoctorAssignmentCommand cmd, CancellationToken ct)
    {
        var patientProfile = await uow.Patients.GetByUserIdAsync(cmd.PatientUserId, ct)
            ?? throw new NotFoundException("Patient", cmd.PatientUserId);

        var existing = await uow.Patients.GetAssignmentAsync(cmd.DoctorUserId, patientProfile.Id, ct)
            ?? throw new NotFoundException("Assignment", $"{cmd.DoctorUserId}/{cmd.PatientUserId}");
        existing.Deactivate();
        uow.Patients.UpdateAssignment(existing);
        await uow.SaveChangesAsync(ct);
    }
}

public class GetAdminOverviewQueryHandler(IUnitOfWork uow) : IRequestHandler<GetAdminOverviewQuery, AdminOverviewDto>
{
    public async Task<AdminOverviewDto> Handle(GetAdminOverviewQuery _, CancellationToken ct)
    {
        var users = (await uow.Users.GetAllAsync(ct)).ToList();
        var doctors = users.Count(u => u.Role == UserRole.Doctor);
        var patients = users.Count(u => u.Role == UserRole.Patient);
        var relatives = users.Count(u => u.Role == UserRole.Relative);

        var allDoctors = await uow.Patients.GetAllDoctorsAsync(ct);
        var activeAssignments = allDoctors
            .Sum(d => d.PatientAssignments.Count(a => a.Status == RelationshipAssignmentStatus.Active));

        return new AdminOverviewDto(
            TotalUsers: users.Count,
            TotalDoctors: doctors,
            TotalPatients: patients,
            TotalRelatives: relatives,
            ActiveAssignments: activeAssignments,
            UnreadNotifications: 0,
            OpenAlerts: 0);
    }
}
