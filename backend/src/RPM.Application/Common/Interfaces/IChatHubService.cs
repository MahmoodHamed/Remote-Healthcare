namespace RPM.Application.Common.Interfaces;

public interface IChatHubService
{
    Task BroadcastMessageAsync(Guid conversationId, object messageDto, CancellationToken ct = default);
}
