using MediatR;
using RPM.Application.Common.Exceptions;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Admin;
using RPM.Domain.Entities;
using RPM.Domain.Enums;
using RPM.Domain.Interfaces;

namespace RPM.Application.Features.Admin;

public class GetAllAdminUsersQueryHandler(IUnitOfWork uow)
    : IRequestHandler<GetAllAdminUsersQuery, IEnumerable<UserAdminDto>>
{
    public async Task<IEnumerable<UserAdminDto>> Handle(GetAllAdminUsersQuery request, CancellationToken ct)
    {
        var users = await uow.Users.GetAllAsync(ct);
        return users.Select(user => new UserAdminDto(
            user.Id, user.FullName, user.Email, user.Phone, user.Role.ToString(),
            user.IsActive, user.AvatarUrl, user.CreatedAt, user.UpdatedAt ?? user.CreatedAt));
    }
}

public class CreateUserAdminCommandHandler(IUnitOfWork uow, IPasswordHasher hasher)
    : IRequestHandler<CreateUserAdminCommand, UserAdminDto>
{
    public async Task<UserAdminDto> Handle(CreateUserAdminCommand cmd, CancellationToken ct)
    {
        if (await uow.Users.ExistsByEmailAsync(cmd.Email, ct))
            throw new ConflictException($"Email '{cmd.Email}' already registered.");

        if (!Enum.TryParse<UserRole>(cmd.Role, out var role))
            throw new FluentValidation.ValidationException(new[] { new FluentValidation.Results.ValidationFailure("Role", "Invalid role.") });

        var user = User.Create(cmd.FullName, cmd.Email, cmd.Phone, hasher.Hash(cmd.Password), role);
        await uow.Users.AddAsync(user, ct);

        if (role == UserRole.Patient)
        {
            var profile = PatientProfile.Create(user.Id);
            await uow.Patients.AddPatientProfileAsync(profile, ct);
            var threshold = AlertThreshold.CreateDefault(profile.Id);
            await uow.Alerts.AddThresholdAsync(threshold, ct);
        }
        else if (role == UserRole.Doctor)
        {
            var doc = DoctorProfile.Create(user.Id, "General", $"ADMIN-{user.Id:N}"[..18]);
            await uow.Patients.AddDoctorProfileAsync(doc, ct);
        }

        await uow.SaveChangesAsync(ct);
        return new UserAdminDto(user.Id, user.FullName, user.Email, user.Phone, user.Role.ToString(), user.IsActive, user.AvatarUrl, user.CreatedAt, user.UpdatedAt ?? user.CreatedAt);
    }
}

public class UpdateUserAdminCommandHandler(IUnitOfWork uow)
    : IRequestHandler<UpdateUserAdminCommand, UserAdminDto>
{
    public async Task<UserAdminDto> Handle(UpdateUserAdminCommand cmd, CancellationToken ct)
    {
        var user = await uow.Users.GetByIdAsync(cmd.UserId, ct)
            ?? throw new NotFoundException(nameof(User), cmd.UserId);

        if (!Enum.TryParse<UserRole>(cmd.Role, out var role))
            throw new FluentValidation.ValidationException(new[] { new FluentValidation.Results.ValidationFailure("Role", "Invalid role.") });

        user.UpdateProfile(cmd.FullName, cmd.Phone);
        user.UpdateRole(role);
        if (cmd.IsActive) user.Activate(); else user.Deactivate();
        uow.Users.Update(user);
        await uow.SaveChangesAsync(ct);

        return new UserAdminDto(user.Id, user.FullName, user.Email, user.Phone, user.Role.ToString(), user.IsActive, user.AvatarUrl, user.CreatedAt, user.UpdatedAt ?? user.CreatedAt);
    }
}

public class DeleteUserAdminCommandHandler(IUnitOfWork uow)
    : IRequestHandler<DeleteUserAdminCommand>
{
    public async Task Handle(DeleteUserAdminCommand cmd, CancellationToken ct)
    {
        var user = await uow.Users.GetByIdAsync(cmd.UserId, ct)
            ?? throw new NotFoundException(nameof(User), cmd.UserId);

        user.Deactivate();
        uow.Users.Update(user);
        await uow.SaveChangesAsync(ct);
    }
}