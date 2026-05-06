using RPM.Domain.Common;
using RPM.Domain.Enums;
using RPM.Domain.Events;
namespace RPM.Domain.Entities;
public class Alert : BaseEntity
{
    public Guid PatientId { get; private set; }
    public User Patient { get; private set; } = null!;
    public Guid VitalRecordId { get; private set; }
    public VitalRecord VitalRecord { get; private set; } = null!;
    public AlertType Type { get; private set; }
    public AlertSeverity Severity { get; private set; }
    public string Message { get; private set; } = string.Empty;
    public AlertStatus Status { get; private set; } = AlertStatus.Unread;
    public Guid? ResolvedById { get; private set; }
    public DateTime TriggeredAt { get; private set; } = DateTime.UtcNow;
    public DateTime? ResolvedAt { get; private set; }

    public ICollection<Notification> Notifications { get; private set; } = [];

    protected Alert() { }

    public static Alert Create(Guid patientId, Guid vitalRecordId, AlertType type, AlertSeverity severity, string message)
    {
        var alert = new Alert
        {
            PatientId = patientId,
            VitalRecordId = vitalRecordId,
            Type = type,
            Severity = severity,
            Message = message
        };
        alert.AddDomainEvent(new AlertTriggeredEvent(alert.Id, patientId, type, severity));
        return alert;
    }

    public void Resolve(Guid resolvedById)
    {
        Status = AlertStatus.Resolved;
        ResolvedById = resolvedById;
        ResolvedAt = DateTime.UtcNow;
        SetUpdatedAt();
    }

    public void MarkRead() { Status = AlertStatus.Read; SetUpdatedAt(); }
    public void Dismiss() { Status = AlertStatus.Dismissed; SetUpdatedAt(); }
}
