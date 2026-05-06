using RPM.Domain.Common;
namespace RPM.Domain.Entities;
public class PatientRelativeLink : BaseEntity
{
    public Guid PatientId { get; private set; }
    public PatientProfile Patient { get; private set; } = null!;
    public Guid RelativeUserId { get; private set; }
    public User RelativeUser { get; private set; } = null!;
    public string Relationship { get; private set; } = string.Empty;
    public bool CanViewVitals { get; private set; } = true;
    public bool CanViewChat { get; private set; } = true;
    public DateTime LinkedAt { get; private set; } = DateTime.UtcNow;

    protected PatientRelativeLink() { }

    public static PatientRelativeLink Create(Guid patientId, Guid relativeUserId, string relationship)
        => new() { PatientId = patientId, RelativeUserId = relativeUserId, Relationship = relationship };

    public void UpdatePermissions(bool canViewVitals, bool canViewChat)
    {
        CanViewVitals = canViewVitals;
        CanViewChat = canViewChat;
        SetUpdatedAt();
    }
}
