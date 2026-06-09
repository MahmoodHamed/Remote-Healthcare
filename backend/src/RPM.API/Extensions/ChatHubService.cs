using Microsoft.AspNetCore.SignalR;
using RPM.API.Hubs;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Chat;

namespace RPM.API.Extensions;

public class ChatHubService(IHubContext<ChatHub> hub) : IChatHubService
{
    public Task BroadcastMessageAsync(MessageDto message, CancellationToken ct = default) =>
        hub.Clients.Group($"conv-{message.ConversationId}")
            .SendAsync("ReceiveMessage", message, ct);
}
