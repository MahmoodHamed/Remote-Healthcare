using RPM.Domain.Common;
namespace RPM.Domain.Entities;
public class RefreshToken : BaseEntity
{
    public Guid UserId { get; private set; }
    public User User { get; private set; } = null!;
    public string TokenHash { get; private set; } = string.Empty;
    public string? DeviceInfo { get; private set; }
    public bool IsRevoked { get; private set; }
    public DateTime ExpiresAt { get; private set; }

    protected RefreshToken() { }

    public static RefreshToken Create(Guid userId, string tokenHash, DateTime expiresAt, string? deviceInfo = null)
        => new() { UserId = userId, TokenHash = tokenHash, ExpiresAt = expiresAt, DeviceInfo = deviceInfo };

    public void Revoke() { IsRevoked = true; SetUpdatedAt(); }
    public bool IsExpired => DateTime.UtcNow >= ExpiresAt;
    public bool IsActive => !IsRevoked && !IsExpired;
}
