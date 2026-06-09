using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using MediatR;
using RPM.Application.Features.Chat.Commands;
using RPM.Application.Common.Interfaces;
using RPM.Domain.Enums;
namespace RPM.API.Hubs;

[Authorize]
public class ChatHub(
    IMediator mediator,
    ICurrentUser currentUser,
    IChatPresenceService presence) : Hub
{
    public async Task JoinConversation(string conversationId)
    {
        if (!Guid.TryParse(conversationId, out var convId)) return;
        await Groups.AddToGroupAsync(Context.ConnectionId, $"conv-{conversationId}");
        await presence.JoinConversationAsync(currentUser.UserId, convId, Context.ConnectionId);
    }

    public async Task LeaveConversation(string conversationId)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"conv-{conversationId}");
        await presence.LeaveConversationAsync(Context.ConnectionId);
    }

    public async Task SendMessage(string conversationId, string content, string type = "Text", string? mediaUrl = null)
    {
        if (!Guid.TryParse(conversationId, out var convId)) return;
        if (!Enum.TryParse<MessageType>(type, out var msgType)) msgType = MessageType.Text;

        var cmd = new SendMessageCommand(convId, currentUser.UserId, content, msgType, mediaUrl);
        await mediator.Send(cmd);
    }

    public Task MarkRead(string conversationId) =>
        Clients.Caller.SendAsync("MarkedRead", conversationId);

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        await presence.LeaveConversationAsync(Context.ConnectionId);
        await base.OnDisconnectedAsync(exception);
    }
}
