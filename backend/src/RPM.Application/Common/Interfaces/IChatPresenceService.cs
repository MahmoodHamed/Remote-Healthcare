namespace RPM.Application.Common.Interfaces;

public interface IChatPresenceService
{
    Task JoinConversationAsync(Guid userId, Guid conversationId, string connectionId, CancellationToken ct = default);
    Task LeaveConversationAsync(string connectionId, CancellationToken ct = default);
    Task<bool> IsUserViewingAsync(Guid userId, Guid conversationId, CancellationToken ct = default);
}
