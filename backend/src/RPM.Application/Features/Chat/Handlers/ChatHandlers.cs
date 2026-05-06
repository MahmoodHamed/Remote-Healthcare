using MediatR;
using RPM.Application.Common.Exceptions;
using RPM.Application.DTOs.Chat;
using RPM.Application.Features.Chat.Commands;
using RPM.Application.Features.Chat.Queries;
using RPM.Domain.Entities;
using RPM.Domain.Interfaces;
namespace RPM.Application.Features.Chat.Handlers;

public class CreateConversationHandler(IUnitOfWork uow)
    : IRequestHandler<CreateConversationCommand, ConversationDto>
{
    public async Task<ConversationDto> Handle(CreateConversationCommand cmd, CancellationToken ct)
    {
        var conv = Conversation.Create(cmd.Type, cmd.Name);
        await uow.Chat.AddConversationAsync(conv, ct);

        foreach (var uid in cmd.ParticipantIds)
        {
            var p = ConversationParticipant.Create(conv.Id, uid, cmd.ParticipantIds.IndexOf(uid) == 0);
            await uow.Chat.AddParticipantAsync(p, ct);
        }
        await uow.SaveChangesAsync(ct);

        // fetch full with participants
        var full = await uow.Chat.GetByIdAsync(conv.Id, ct) ?? throw new NotFoundException(nameof(Conversation), conv.Id);
        return ChatMapper.MapConv(full);
    }
}

public class SendMessageHandler(IUnitOfWork uow)
    : IRequestHandler<SendMessageCommand, MessageDto>
{
    public async Task<MessageDto> Handle(SendMessageCommand cmd, CancellationToken ct)
    {
        var conv = await uow.Chat.GetByIdAsync(cmd.ConversationId, ct)
            ?? throw new NotFoundException(nameof(Conversation), cmd.ConversationId);

        var msg = Message.Create(cmd.ConversationId, cmd.SenderId, cmd.Content, cmd.Type, cmd.MediaUrl);
        await uow.Chat.AddMessageAsync(msg, ct);
        conv.UpdateLastMessageAt();
        uow.Chat.UpdateConversation(conv);
        await uow.SaveChangesAsync(ct);

        var sender = await uow.Users.GetByIdAsync(cmd.SenderId, ct);
        return new MessageDto(msg.Id, msg.ConversationId, msg.SenderId,
            sender?.FullName ?? "Unknown", msg.Content, msg.Type.ToString(),
            msg.MediaUrl, msg.IsDeleted, msg.SentAt);
    }
}

public class DeleteMessageHandler(IUnitOfWork uow)
    : IRequestHandler<DeleteMessageCommand>
{
    public async Task Handle(DeleteMessageCommand cmd, CancellationToken ct)
    {
        var msg = await uow.Chat.GetMessageByIdAsync(cmd.MessageId, ct)
            ?? throw new NotFoundException(nameof(Message), cmd.MessageId);
        if (msg.SenderId != cmd.RequestingUserId) throw new ForbiddenException();
        msg.Delete();
        await uow.SaveChangesAsync(ct);
    }
}

public class GetMyConversationsHandler(IUnitOfWork uow)
    : IRequestHandler<GetMyConversationsQuery, IEnumerable<ConversationDto>>
{
    public async Task<IEnumerable<ConversationDto>> Handle(GetMyConversationsQuery q, CancellationToken ct)
    {
        var convs = await uow.Chat.GetByUserIdAsync(q.UserId, ct);
        return convs.Select(ChatMapper.MapConv);
    }
}

public class GetConversationMessagesHandler(IUnitOfWork uow)
    : IRequestHandler<GetConversationMessagesQuery, MessagePagedDto>
{
    public async Task<MessagePagedDto> Handle(GetConversationMessagesQuery q, CancellationToken ct)
    {
        var msgs = await uow.Chat.GetMessagesByConversationIdAsync(q.ConversationId, q.Page, q.PageSize, ct);
        var dtos = msgs.Select(m => new MessageDto(m.Id, m.ConversationId, m.SenderId,
            m.Sender?.FullName ?? "Unknown", m.Content, m.Type.ToString(), m.MediaUrl, m.IsDeleted, m.SentAt));
        return new MessagePagedDto(dtos, dtos.Count(), q.Page, q.PageSize);
    }
}

file static class ChatMapper
{
    public static ConversationDto MapConv(Conversation c) =>
        new(c.Id, c.Type.ToString(), c.Name, c.LastMessageAt,
            c.Participants.Select(p => new ParticipantDto(p.UserId, p.User?.FullName ?? "", p.User?.AvatarUrl, p.IsAdmin)).ToList());
}
