using RPM.Domain.Entities;

namespace RPM.Application.Common.Interfaces;

/// <summary>Unified refresh-token lifecycle: issue, validate, and revoke.</summary>
public interface IRefreshTokenService
{
    Task<string> IssueAsync(Guid userId, string? deviceInfo, CancellationToken ct = default);
    Task<RefreshToken?> FindActiveAsync(string plainToken, CancellationToken ct = default);
    Task RevokeAsync(string plainToken, CancellationToken ct = default);
}
