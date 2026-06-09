using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using MediatR;
using RPM.Application.Features.Chat.Commands;
using RPM.Application.Common.Interfaces;
using RPM.Domain.Enums;
namespace RPM.API.Hubs;

[Authorize]
public class ChatHub(IMediator mediator, ICurrentUser currentUser) : Hub
{
    public async Task JoinConversation(string conversationId) =>
        await Groups.AddToGroupAsync(Context.ConnectionId, $"conv-{conversationId}");

    public async Task LeaveConversation(string conversationId) =>
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"conv-{conversationId}");

    public async Task SendMessage(string conversationId, string content, string type = "Text", string? mediaUrl = null)
    {
        if (!Guid.TryParse(conversationId, out var convId)) return;
        if (!Enum.TryParse<MessageType>(type, out var msgType)) msgType = MessageType.Text;

        var cmd = new SendMessageCommand(convId, currentUser.UserId, content, msgType, mediaUrl);
        var msg = await mediator.Send(cmd);

        // Broadcast to all participants in the conversation
        await Clients.Group($"conv-{conversationId}").SendAsync("ReceiveMessage", msg);
    }

    public async Task MarkRead(string conversationId) =>
        await Clients.Caller.SendAsync("MarkedRead", conversationId);
}
