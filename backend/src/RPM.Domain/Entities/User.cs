using RPM.Domain.Common;
using RPM.Domain.Enums;
namespace RPM.Domain.Entities;
public class User : BaseEntity
{
    public string FullName { get; private set; } = string.Empty;
    public string Email { get; private set; } = string.Empty;
    public string Phone { get; private set; } = string.Empty;
    public string PasswordHash { get; private set; } = string.Empty;
    public UserRole Role { get; private set; }
    public string? FcmToken { get; private set; }
    public string? AvatarUrl { get; private set; }
    public bool IsActive { get; private set; } = true;

    public DoctorProfile? DoctorProfile { get; private set; }
    public PatientProfile? PatientProfile { get; private set; }
    public ICollection<RefreshToken> RefreshTokens { get; private set; } = [];
    public ICollection<Notification> Notifications { get; private set; } = [];

    protected User() { }

    public static User Create(string fullName, string email, string phone, string passwordHash, UserRole role)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(fullName);
        ArgumentException.ThrowIfNullOrWhiteSpace(email);
        return new User
        {
            FullName = fullName,
            Email = email.ToLowerInvariant(),
            Phone = phone,
            PasswordHash = passwordHash,
            Role = role
        };
    }

    public void UpdateFcmToken(string token) { FcmToken = token; SetUpdatedAt(); }
    public void UpdateAvatar(string url) { AvatarUrl = url; SetUpdatedAt(); }
    public void Deactivate() { IsActive = false; SetUpdatedAt(); }
    public void Activate() { IsActive = true; SetUpdatedAt(); }
}
