using RPM.Domain.Common;
namespace RPM.Domain.Entities;
public class Notification : BaseEntity
{
    public Guid UserId { get; private set; }
    public User User { get; private set; } = null!;
    public Guid? AlertId { get; private set; }
    public Alert? Alert { get; private set; }
    public string Title { get; private set; } = string.Empty;
    public string Body { get; private set; } = string.Empty;
    public string? DataPayload { get; private set; }
    public bool IsRead { get; private set; }
    public DateTime SentAt { get; private set; } = DateTime.UtcNow;

    protected Notification() { }

    public static Notification Create(Guid userId, string title, string body,
        Guid? alertId = null, string? data = null)
        => new() { UserId = userId, Title = title, Body = body, AlertId = alertId, DataPayload = data };

    public void MarkRead() { IsRead = true; SetUpdatedAt(); }
}
