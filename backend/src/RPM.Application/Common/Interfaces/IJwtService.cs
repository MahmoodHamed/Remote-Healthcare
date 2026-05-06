namespace RPM.Application.Common.Interfaces;
public interface IJwtService
{
    string GenerateAccessToken(Guid userId, string email, string role);
    (bool IsValid, Guid UserId) ValidateRefreshToken(string token);
    string GenerateRefreshToken();
}
