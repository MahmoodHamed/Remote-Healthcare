using RPM.Domain.Common;
using RPM.Domain.Enums;
using RPM.Domain.Events;
namespace RPM.Domain.Entities;
public class Conversation : BaseEntity
{
    public ConversationType Type { get; private set; }
    public string? Name { get; private set; }
    public DateTime? LastMessageAt { get; private set; }

    public ICollection<ConversationParticipant> Participants { get; private set; } = [];
    public ICollection<Message> Messages { get; private set; } = [];

    protected Conversation() { }

    public static Conversation Create(ConversationType type, string? name = null)
        => new() { Type = type, Name = name };

    public void UpdateLastMessageAt() { LastMessageAt = DateTime.UtcNow; SetUpdatedAt(); }
}
