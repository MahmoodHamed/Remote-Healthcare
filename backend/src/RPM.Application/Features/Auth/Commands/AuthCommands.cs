using MediatR;
using RPM.Application.DTOs.Auth;
namespace RPM.Application.Features.Auth.Commands;

/// <summary>Legacy single endpoint kept for backwards compatibility.</summary>
public record RegisterCommand(
    string FullName,
    string Email,
    string Phone,
    string Password,
    string Role,
    string? LicenseNumber,
    string? Specialization) : IRequest<LoginResponseDto>;

public record RegisterPatientCommand(
    string FullName,
    string Email,
    string Phone,
    string Password) : IRequest<LoginResponseDto>;

public record RegisterDoctorCommand(
    string FullName,
    string Email,
    string Phone,
    string Password,
    string LicenseNumber,
    string Specialization,
    string? HospitalName) : IRequest<LoginResponseDto>;

public record LoginCommand(string Email, string Password, string? DeviceInfo) : IRequest<LoginResponseDto>;
public record AdminLoginCommand(string Email, string Password, string? DeviceInfo) : IRequest<LoginResponseDto>;
public record RefreshTokenCommand(string RefreshToken, string? AccessToken = null, string? DeviceInfo = null) : IRequest<AuthTokensDto>;
public record LogoutCommand(string RefreshToken) : IRequest;
public record UpdateFcmTokenCommand(string FcmToken) : IRequest;
