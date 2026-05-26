using MediatR;
using RPM.Application.Common.Exceptions;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Auth;
using RPM.Application.Features.Auth.Commands;
using RPM.Domain.Entities;
using RPM.Domain.Enums;
using RPM.Domain.Interfaces;
namespace RPM.Application.Features.Auth.Handlers;

public class RegisterCommandHandler(IUnitOfWork uow, IPasswordHasher hasher, IJwtService jwt)
    : IRequestHandler<RegisterCommand, LoginResponseDto>
{
    public async Task<LoginResponseDto> Handle(RegisterCommand cmd, CancellationToken ct)
    {
        if (!Enum.TryParse<UserRole>(cmd.Role, out var role) || role == UserRole.Admin)
            throw new FluentValidation.ValidationException(new[]
            {
                new FluentValidation.Results.ValidationFailure("Role", "Invalid role for public registration.")
            });

        return await AuthRegistration.CreateUserAsync(
            uow, hasher, jwt,
            cmd.FullName, cmd.Email, cmd.Phone, cmd.Password,
            role,
            cmd.LicenseNumber,
            cmd.Specialization,
            null,
            ct);
    }
}

public class RegisterPatientCommandHandler(IUnitOfWork uow, IPasswordHasher hasher, IJwtService jwt)
    : IRequestHandler<RegisterPatientCommand, LoginResponseDto>
{
    public async Task<LoginResponseDto> Handle(RegisterPatientCommand cmd, CancellationToken ct) =>
        await AuthRegistration.CreateUserAsync(
            uow, hasher, jwt,
            cmd.FullName, cmd.Email, cmd.Phone, cmd.Password,
            UserRole.Patient,
            null, null, null, ct);
}

public class RegisterDoctorCommandHandler(IUnitOfWork uow, IPasswordHasher hasher, IJwtService jwt)
    : IRequestHandler<RegisterDoctorCommand, LoginResponseDto>
{
    public async Task<LoginResponseDto> Handle(RegisterDoctorCommand cmd, CancellationToken ct) =>
        await AuthRegistration.CreateUserAsync(
            uow, hasher, jwt,
            cmd.FullName, cmd.Email, cmd.Phone, cmd.Password,
            UserRole.Doctor,
            cmd.LicenseNumber,
            cmd.Specialization,
            cmd.HospitalName,
            ct);
}

public class LoginCommandHandler(IUnitOfWork uow, IPasswordHasher hasher, IJwtService jwt)
    : IRequestHandler<LoginCommand, LoginResponseDto>
{
    public async Task<LoginResponseDto> Handle(LoginCommand cmd, CancellationToken ct) =>
        await AuthRegistration.SignInAsync(uow, hasher, jwt, cmd.Email, cmd.Password, cmd.DeviceInfo, requireAdmin: false, ct);
}

public class AdminLoginCommandHandler(IUnitOfWork uow, IPasswordHasher hasher, IJwtService jwt)
    : IRequestHandler<AdminLoginCommand, LoginResponseDto>
{
    public async Task<LoginResponseDto> Handle(AdminLoginCommand cmd, CancellationToken ct) =>
        await AuthRegistration.SignInAsync(uow, hasher, jwt, cmd.Email, cmd.Password, cmd.DeviceInfo, requireAdmin: true, ct);
}

public class UpdateFcmTokenCommandHandler(IUnitOfWork uow, ICurrentUser currentUser)
    : IRequestHandler<UpdateFcmTokenCommand>
{
    public async Task Handle(UpdateFcmTokenCommand cmd, CancellationToken ct)
    {
        var user = await uow.Users.GetByIdAsync(currentUser.UserId, ct)
            ?? throw new NotFoundException(nameof(User), currentUser.UserId);
        user.UpdateFcmToken(cmd.FcmToken);
        uow.Users.Update(user);
        await uow.SaveChangesAsync(ct);
    }
}

internal static class AuthRegistration
{
    public static async Task<LoginResponseDto> CreateUserAsync(
        IUnitOfWork uow,
        IPasswordHasher hasher,
        IJwtService jwt,
        string fullName,
        string email,
        string phone,
        string password,
        UserRole role,
        string? licenseNumber,
        string? specialization,
        string? hospitalName,
        CancellationToken ct)
    {
        if (await uow.Users.ExistsByEmailAsync(email, ct))
            throw new ConflictException($"Email '{email}' already registered.");

        var user = User.Create(fullName, email, phone, hasher.Hash(password), role);
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
            var license = string.IsNullOrWhiteSpace(licenseNumber)
                ? $"LIC-{user.Id:N}"[..18]
                : licenseNumber;
            var doc = DoctorProfile.Create(user.Id, specialization ?? "General", license, hospitalName);
            await uow.Patients.AddDoctorProfileAsync(doc, ct);
        }

        await uow.SaveChangesAsync(ct);

        var accessToken = jwt.GenerateAccessToken(user.Id, user.Email, user.Role.ToString());
        var refreshToken = jwt.GenerateRefreshToken();
        return new LoginResponseDto(
            new AuthTokensDto(accessToken, refreshToken, DateTime.UtcNow.AddHours(1)),
            new UserProfileDto(user.Id, user.FullName, user.Email, user.Phone, user.Role.ToString(), null));
    }

    public static async Task<LoginResponseDto> SignInAsync(
        IUnitOfWork uow,
        IPasswordHasher hasher,
        IJwtService jwt,
        string email,
        string password,
        string? deviceInfo,
        bool requireAdmin,
        CancellationToken ct)
    {
        var user = await uow.Users.GetByEmailAsync(email, ct)
            ?? throw new UnauthorizedException("Invalid email or password.");

        if (!user.IsActive) throw new UnauthorizedException("Account is deactivated.");
        if (!hasher.Verify(password, user.PasswordHash))
            throw new UnauthorizedException("Invalid email or password.");

        if (requireAdmin && user.Role != UserRole.Admin)
            throw new UnauthorizedException("This sign-in page is reserved for administrators.");

        var accessToken = jwt.GenerateAccessToken(user.Id, user.Email, user.Role.ToString());
        var refreshToken = jwt.GenerateRefreshToken();
        _ = RefreshToken.Create(user.Id, hasher.Hash(refreshToken), DateTime.UtcNow.AddDays(30), deviceInfo);
        await uow.SaveChangesAsync(ct);

        return new LoginResponseDto(
            new AuthTokensDto(accessToken, refreshToken, DateTime.UtcNow.AddHours(1)),
            new UserProfileDto(user.Id, user.FullName, user.Email, user.Phone, user.Role.ToString(), user.AvatarUrl));
    }
}
