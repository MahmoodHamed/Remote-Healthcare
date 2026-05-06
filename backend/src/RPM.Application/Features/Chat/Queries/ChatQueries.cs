using MediatR;
using RPM.Application.DTOs.Chat;
namespace RPM.Application.Features.Chat.Queries;
public record GetMyConversationsQuery(Guid UserId) : IRequest<IEnumerable<ConversationDto>>;
public record GetConversationMessagesQuery(Guid ConversationId, int Page = 1, int PageSize = 50) : IRequest<MessagePagedDto>;
