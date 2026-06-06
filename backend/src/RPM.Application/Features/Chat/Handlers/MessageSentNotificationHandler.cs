using MediatR;
using RPM.Application.Common.Interfaces;
using RPM.Application.Features.Chat.Notifications;
using RPM.Domain.Entities;
using RPM.Domain.Interfaces;

namespace RPM.Application.Features.Chat.Handlers;

public class MessageSentNotificationHandler(
    IUnitOfWork uow,
    IChatHubService chatHub,
    IChatPresenceService presence,
    INotificationService push) : INotificationHandler<MessageSentNotification>
{
    public async Task Handle(MessageSentNotification notification, CancellationToken ct)
    {
        var msg = notification.Message;

        await chatHub.BroadcastMessageAsync(msg.ConversationId, msg, ct);

        var conv = await uow.Chat.GetByIdAsync(msg.ConversationId, ct);
        if (conv is null) return;

        var preview = msg.Content.Length > 120 ? msg.Content[..120] + "…" : msg.Content;
        var title = $"Message from {msg.SenderName}";
        var dataPayload =
            $"{{\"conversationId\":\"{msg.ConversationId}\",\"messageId\":\"{msg.Id}\",\"type\":\"ChatMessage\"}}";

        foreach (var participant in conv.Participants.Where(p => p.UserId != msg.SenderId))
        {
            if (await presence.IsUserViewingAsync(participant.UserId, msg.ConversationId, ct))
                continue;

            var user = await uow.Users.GetByIdAsync(participant.UserId, ct);
            if (user is null) continue;

            await uow.Users.AddNotificationAsync(
                Notification.Create(participant.UserId, title, preview, null, dataPayload), ct);

            if (!string.IsNullOrWhiteSpace(user.FcmToken))
            {
                try
                {
                    await push.SendPushAsync(user.FcmToken!, title, preview,
                        new Dictionary<string, string>
                        {
                            ["conversationId"] = msg.ConversationId.ToString(),
                            ["messageId"] = msg.Id.ToString(),
                            ["type"] = "ChatMessage",
                            ["channelId"] = "rpm_chat"
                        },
                        channelId: "rpm_chat", ct);
                }
                catch
                {
                    /* FCM optional — inbox notification already saved */
                }
            }
        }

        await uow.SaveChangesAsync(ct);
    }
}
