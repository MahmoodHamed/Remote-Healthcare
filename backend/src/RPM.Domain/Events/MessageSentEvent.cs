using RPM.Domain.Common;
namespace RPM.Domain.Events;
public record MessageSentEvent(Guid MessageId, Guid ConversationId, Guid SenderId) : IDomainEvent;
