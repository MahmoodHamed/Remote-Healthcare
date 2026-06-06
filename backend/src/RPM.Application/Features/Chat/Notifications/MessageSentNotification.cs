using MediatR;
using RPM.Application.DTOs.Chat;

namespace RPM.Application.Features.Chat.Notifications;

public record MessageSentNotification(MessageDto Message) : INotification;
