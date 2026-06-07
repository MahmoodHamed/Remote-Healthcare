using MediatR;
using RPM.Application.Common;
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
        if (await uow.Users.ExistsByEmailAsync(cmd.Email, ct))
            throw new ConflictException($"Email '{cmd.Email}' already registered.");

        if (!Enum.TryParse<UserRole>(cmd.Role, out var role))
            throw new FluentValidation.ValidationException(new[] { new FluentValidation.Results.ValidationFailure("Role", "Invalid role.") });

        var user = User.Create(cmd.FullName, cmd.Email, cmd.Phone, hasher.Hash(cmd.Password), role);
        await uow.Users.AddAsync(user, ct);

        if (role == UserRole.Patient)
        {
            var profile = PatientProfile.Create(user.Id);
            for (var salt = 0; salt < 20; salt++)
            {
                var code = PatientShortCode.Generate(user.Id, salt);
                if (!await uow.Patients.ShortPatientCodeExistsAsync(code, ct))
                {
                    profile.AssignShortPatientCode(code);
                    break;
                }
            }
            await uow.Patients.AddPatientProfileAsync(profile, ct);
            var threshold = AlertThreshold.CreateDefault(profile.Id);
            await uow.Alerts.AddThresholdAsync(threshold, ct);
        }
        else if (role == UserRole.Doctor && cmd.LicenseNumber != null)
        {
            var doc = DoctorProfile.Create(user.Id, cmd.Specialization ?? "General", cmd.LicenseNumber);
            await uow.Patients.AddDoctorProfileAsync(doc, ct);
        }

        await uow.SaveChangesAsync(ct);

        var accessToken = jwt.GenerateAccessToken(user.Id, user.Email, user.Role.ToString());
        var refreshToken = jwt.GenerateRefreshToken();
        var expiry = DateTime.UtcNow.AddDays(30);
        var rt = RefreshToken.Create(user.Id, hasher.Hash(refreshToken), expiry);
        await uow.Users.AddRefreshTokenAsync(rt, ct);
        await uow.SaveChangesAsync(ct);

        return new LoginResponseDto(
            new AuthTokensDto(accessToken, refreshToken, DateTime.UtcNow.AddHours(1)),
            new UserProfileDto(user.Id, user.FullName, user.Email, user.Phone, user.Role.ToString(), null));
    }
}

public class LoginCommandHandler(IUnitOfWork uow, IPasswordHasher hasher, IJwtService jwt)
    : IRequestHandler<LoginCommand, LoginResponseDto>
{
    public async Task<LoginResponseDto> Handle(LoginCommand cmd, CancellationToken ct)
    {
        var user = await uow.Users.GetByEmailAsync(cmd.Email, ct)
            ?? throw new UnauthorizedException("Invalid email or password.");

        if (!user.IsActive) throw new UnauthorizedException("Account is deactivated.");
        if (!hasher.Verify(cmd.Password, user.PasswordHash)) throw new UnauthorizedException("Invalid email or password.");

        var accessToken = jwt.GenerateAccessToken(user.Id, user.Email, user.Role.ToString());
        var refreshToken = jwt.GenerateRefreshToken();
        var rt = RefreshToken.Create(user.Id, hasher.Hash(refreshToken), DateTime.UtcNow.AddDays(30), cmd.DeviceInfo);
        await uow.Users.AddRefreshTokenAsync(rt, ct);
        await uow.SaveChangesAsync(ct);

        return new LoginResponseDto(
            new AuthTokensDto(accessToken, refreshToken, DateTime.UtcNow.AddHours(1)),
            new UserProfileDto(user.Id, user.FullName, user.Email, user.Phone, user.Role.ToString(), user.AvatarUrl));
    }
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
