using RPM.Domain.Common;
namespace RPM.Domain.Entities;
public class MessageRead : BaseEntity
{
    public Guid MessageId { get; private set; }
    public Message Message { get; private set; } = null!;
    public Guid UserId { get; private set; }
    public User User { get; private set; } = null!;
    public DateTime ReadAt { get; private set; } = DateTime.UtcNow;

    protected MessageRead() { }

    public static MessageRead Create(Guid messageId, Guid userId)
        => new() { MessageId = messageId, UserId = userId };
}
