using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;
using RPM.Application.Common.Interfaces;
namespace RPM.Infrastructure.Services;

public class JwtService(IConfiguration config) : IJwtService
{
    private readonly string _secret = config["Jwt:Secret"] ?? throw new InvalidOperationException("Jwt:Secret not configured");
    private readonly string _issuer = config["Jwt:Issuer"] ?? "RPM";
    private readonly string _audience = config["Jwt:Audience"] ?? "RPM";
    private readonly int _expiryHours = int.Parse(config["Jwt:ExpiryHours"] ?? "1");

    public string GenerateAccessToken(Guid userId, string email, string role)
    {
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_secret));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, userId.ToString()),
            new Claim(ClaimTypes.Email, email),
            new Claim(ClaimTypes.Role, role),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
        };
        var token = new JwtSecurityToken(_issuer, _audience, claims,
            expires: DateTime.UtcNow.AddHours(_expiryHours),
            signingCredentials: credentials);
        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public string GenerateRefreshToken() =>
        Convert.ToBase64String(RandomNumberGenerator.GetBytes(64));

    public (bool IsValid, Guid UserId) ValidateRefreshToken(string token) =>
        throw new NotImplementedException("Handled by looking up hash in DB.");
}
