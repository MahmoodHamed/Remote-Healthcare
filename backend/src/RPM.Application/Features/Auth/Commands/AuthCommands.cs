using MediatR;
using RPM.Application.DTOs.Auth;
namespace RPM.Application.Features.Auth.Commands;
public record RegisterCommand(string FullName, string Email, string Phone, string Password, string Role, string? LicenseNumber, string? Specialization) : IRequest<LoginResponseDto>;
public record LoginCommand(string Email, string Password, string? DeviceInfo) : IRequest<LoginResponseDto>;
public record RefreshTokenCommand(string AccessToken, string RefreshToken, string? DeviceInfo) : IRequest<AuthTokensDto>;
public record LogoutCommand(string RefreshToken) : IRequest;
public record UpdateFcmTokenCommand(Guid UserId, string FcmToken) : IRequest;
