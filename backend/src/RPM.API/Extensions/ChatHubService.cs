using Microsoft.AspNetCore.SignalR;
using RPM.API.Hubs;
using RPM.Application.Common.Interfaces;

namespace RPM.API.Extensions;

public class ChatHubService(IHubContext<ChatHub> hub) : IChatHubService
{
    public Task BroadcastMessageAsync(Guid conversationId, object messageDto, CancellationToken ct = default) =>
        hub.Clients.Group($"conv-{conversationId}").SendAsync("ReceiveMessage", messageDto, ct);
}
