namespace RPM.Application.DTOs.Auth;
public record AuthTokensDto(string AccessToken, string RefreshToken, DateTime ExpiresAt);
public record UserProfileDto(Guid Id, string FullName, string Email, string Phone, string Role, string? AvatarUrl);
public record LoginResponseDto(AuthTokensDto Tokens, UserProfileDto User);
