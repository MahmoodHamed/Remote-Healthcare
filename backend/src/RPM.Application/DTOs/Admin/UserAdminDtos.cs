namespace RPM.Application.DTOs.Admin;

public record UserAdminDto(
    Guid Id,
    string FullName,
    string Email,
    string Phone,
    string Role,
    bool IsActive,
    string? AvatarUrl,
    DateTime CreatedAt,
    DateTime UpdatedAt);

public record UpsertUserAdminRequest(
    string FullName,
    string Email,
    string Phone,
    string Password,
    string Role);

public record UpdateUserAdminRequest(
    string FullName,
    string Phone,
    string Role,
    bool IsActive);