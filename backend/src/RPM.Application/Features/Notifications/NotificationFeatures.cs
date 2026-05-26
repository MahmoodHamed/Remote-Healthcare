using MediatR;
using RPM.Application.Common.Exceptions;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Notifications;
using RPM.Domain.Interfaces;

namespace RPM.Application.Features.Notifications;

public record GetMyNotificationsQuery(int Page = 1, int PageSize = 30) : IRequest<NotificationsPagedDto>;
public record GetUnreadNotificationsCountQuery() : IRequest<int>;
public record MarkNotificationReadCommand(Guid NotificationId) : IRequest;
public record MarkAllNotificationsReadCommand() : IRequest;

public class GetMyNotificationsQueryHandler(IUnitOfWork uow, ICurrentUser current)
    : IRequestHandler<GetMyNotificationsQuery, NotificationsPagedDto>
{
    public async Task<NotificationsPagedDto> Handle(GetMyNotificationsQuery q, CancellationToken ct)
    {
        var page = Math.Max(1, q.Page);
        var pageSize = Math.Clamp(q.PageSize, 1, 100);
        var items = await uow.Notifications.GetByUserIdAsync(current.UserId, page, pageSize, ct);
        var unread = await uow.Notifications.GetUnreadCountAsync(current.UserId, ct);
        var dtos = items
            .Select(n => new NotificationDto(n.Id, n.Title, n.Body, n.IsRead, n.SentAt, n.AlertId, n.DataPayload))
            .ToList();
        return new NotificationsPagedDto(dtos, unread, page, pageSize);
    }
}

public class GetUnreadNotificationsCountQueryHandler(IUnitOfWork uow, ICurrentUser current)
    : IRequestHandler<GetUnreadNotificationsCountQuery, int>
{
    public async Task<int> Handle(GetUnreadNotificationsCountQuery _, CancellationToken ct) =>
        await uow.Notifications.GetUnreadCountAsync(current.UserId, ct);
}

public class MarkNotificationReadCommandHandler(IUnitOfWork uow, ICurrentUser current)
    : IRequestHandler<MarkNotificationReadCommand>
{
    public async Task Handle(MarkNotificationReadCommand cmd, CancellationToken ct)
    {
        var notif = await uow.Notifications.GetByIdAsync(cmd.NotificationId, ct)
            ?? throw new NotFoundException("Notification", cmd.NotificationId);
        if (notif.UserId != current.UserId)
            throw new UnauthorizedException("Not allowed to update this notification.");
        notif.MarkRead();
        uow.Notifications.Update(notif);
        await uow.SaveChangesAsync(ct);
    }
}

public class MarkAllNotificationsReadCommandHandler(IUnitOfWork uow, ICurrentUser current)
    : IRequestHandler<MarkAllNotificationsReadCommand>
{
    public async Task Handle(MarkAllNotificationsReadCommand _, CancellationToken ct)
    {
        await uow.Notifications.MarkAllReadAsync(current.UserId, ct);
        await uow.SaveChangesAsync(ct);
    }
}
