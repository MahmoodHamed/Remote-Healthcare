using RPM.Domain.Common;
namespace RPM.Domain.Entities;
public class ConversationParticipant : BaseEntity
{
    public Guid ConversationId { get; private set; }
    public Conversation Conversation { get; private set; } = null!;
    public Guid UserId { get; private set; }
    public User User { get; private set; } = null!;
    public bool IsAdmin { get; private set; }
    public DateTime JoinedAt { get; private set; } = DateTime.UtcNow;
    public DateTime? LastReadAt { get; private set; }

    protected ConversationParticipant() { }

    public static ConversationParticipant Create(Guid conversationId, Guid userId, bool isAdmin = false)
        => new() { ConversationId = conversationId, UserId = userId, IsAdmin = isAdmin };

    public void MarkRead() { LastReadAt = DateTime.UtcNow; SetUpdatedAt(); }
}
