using RPM.Application.Common.Interfaces;
using RPM.Domain.Entities;
using RPM.Domain.Interfaces;

namespace RPM.Infrastructure.Services;

public class RefreshTokenService(IUnitOfWork uow, ITokenHasher tokenHasher, IJwtService jwt) : IRefreshTokenService
{
    private const int RefreshTokenLifetimeDays = 30;

    public async Task<string> IssueAsync(Guid userId, string? deviceInfo, CancellationToken ct = default)
    {
        var plainToken = jwt.GenerateRefreshToken();
        var entity = RefreshToken.Create(
            userId,
            tokenHasher.HashToken(plainToken),
            DateTime.UtcNow.AddDays(RefreshTokenLifetimeDays),
            deviceInfo);
        await uow.RefreshTokens.AddAsync(entity, ct);
        return plainToken;
    }

    public async Task<RefreshToken?> FindActiveAsync(string plainToken, CancellationToken ct = default)
    {
        var stored = await uow.RefreshTokens.GetByTokenHashAsync(tokenHasher.HashToken(plainToken), ct);
        return stored is { IsActive: true } ? stored : null;
    }

    public async Task RevokeAsync(string plainToken, CancellationToken ct = default)
    {
        var stored = await uow.RefreshTokens.GetByTokenHashAsync(tokenHasher.HashToken(plainToken), ct);
        if (stored is null) return;
        stored.Revoke();
        uow.RefreshTokens.Update(stored);
        await uow.SaveChangesAsync(ct);
    }
}
