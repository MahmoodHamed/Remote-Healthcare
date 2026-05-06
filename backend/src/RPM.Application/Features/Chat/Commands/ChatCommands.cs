using MediatR;
using RPM.Application.DTOs.Chat;
using RPM.Domain.Enums;
namespace RPM.Application.Features.Chat.Commands;

public record CreateConversationCommand(ConversationType Type, string? Name, List<Guid> ParticipantIds) : IRequest<ConversationDto>;
public record SendMessageCommand(Guid ConversationId, Guid SenderId, string Content, MessageType Type = MessageType.Text, string? MediaUrl = null) : IRequest<MessageDto>;
public record DeleteMessageCommand(Guid MessageId, Guid RequestingUserId) : IRequest;
