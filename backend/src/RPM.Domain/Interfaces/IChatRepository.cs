using RPM.Domain.Entities;
namespace RPM.Domain.Interfaces;
public interface IChatRepository
{
    Task<Conversation?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<IEnumerable<Conversation>> GetByUserIdAsync(Guid userId, CancellationToken ct = default);
    Task<Conversation?> GetDirectConversationAsync(Guid userId1, Guid userId2, CancellationToken ct = default);
    Task<IEnumerable<Message>> GetMessagesByConversationIdAsync(Guid conversationId, int page, int pageSize, CancellationToken ct = default);
    Task<Message?> GetMessageByIdAsync(Guid messageId, CancellationToken ct = default);
    Task AddConversationAsync(Conversation conversation, CancellationToken ct = default);
    Task AddMessageAsync(Message message, CancellationToken ct = default);
    Task AddParticipantAsync(ConversationParticipant participant, CancellationToken ct = default);
    void UpdateConversation(Conversation conversation);
}
