using RPM.Domain.Common;
using RPM.Domain.Enums;
using RPM.Domain.Events;
namespace RPM.Domain.Entities;
public class Message : BaseEntity
{
    public Guid ConversationId { get; private set; }
    public Conversation Conversation { get; private set; } = null!;
    public Guid SenderId { get; private set; }
    public User Sender { get; private set; } = null!;
    public string Content { get; private set; } = string.Empty;
    public MessageType Type { get; private set; } = MessageType.Text;
    public string? MediaUrl { get; private set; }
    public bool IsDeleted { get; private set; }
    public DateTime SentAt { get; private set; } = DateTime.UtcNow;
    public DateTime? EditedAt { get; private set; }

    public ICollection<MessageRead> ReadBy { get; private set; } = [];

    protected Message() { }

    public static Message Create(Guid conversationId, Guid senderId, string content,
        MessageType type = MessageType.Text, string? mediaUrl = null)
    {
        var msg = new Message
        {
            ConversationId = conversationId,
            SenderId = senderId,
            Content = content,
            Type = type,
            MediaUrl = mediaUrl
        };
        msg.AddDomainEvent(new MessageSentEvent(msg.Id, conversationId, senderId));
        return msg;
    }

    public void Delete() { IsDeleted = true; SetUpdatedAt(); }
    public void Edit(string newContent) { Content = newContent; EditedAt = DateTime.UtcNow; SetUpdatedAt(); }
}
