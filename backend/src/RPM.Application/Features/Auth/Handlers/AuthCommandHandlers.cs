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
        // track the refresh token (uow is tracking user already, attach new token via separate repo)
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
        // note: refresh token is persisted by EF change tracking
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
        var user = await uow.Users.GetByIdAsync(cmd.UserId, ct)
            ?? throw new NotFoundException(nameof(User), cmd.UserId);
        user.UpdateFcmToken(cmd.FcmToken);
        uow.Users.Update(user);
        await uow.SaveChangesAsync(ct);
    }
}
