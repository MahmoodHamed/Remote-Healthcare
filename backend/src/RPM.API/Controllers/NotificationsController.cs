using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.Common.Interfaces;
using RPM.Domain.Interfaces;

namespace RPM.API.Controllers;

[ApiController]
[Route("api/notifications")]
[Authorize]
public class NotificationsController(IUnitOfWork uow, ICurrentUser currentUser) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetNotifications(
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 30,
        CancellationToken ct = default)
    {
        var items = await uow.Users.GetNotificationsAsync(currentUser.UserId, page, pageSize, ct);
        var unreadCount = await uow.Users.GetUnreadNotificationCountAsync(currentUser.UserId, ct);
        return Ok(new
        {
            items = items.Select(n => new
            {
                id = n.Id,
                title = n.Title,
                body = n.Body,
                alertId = n.AlertId,
                isRead = n.IsRead,
                sentAt = n.SentAt,
            }),
            unreadCount,
            page,
            pageSize,
        });
    }

    [HttpGet("unread-count")]
    public async Task<IActionResult> GetUnreadCount(CancellationToken ct = default)
    {
        var count = await uow.Users.GetUnreadNotificationCountAsync(currentUser.UserId, ct);
        return Ok(new { count });
    }

    [HttpPatch("{id:guid}/read")]
    public async Task<IActionResult> MarkRead(Guid id, CancellationToken ct = default)
    {
        var notification = await uow.Users.GetNotificationByIdAsync(id, ct);
        if (notification is null || notification.UserId != currentUser.UserId)
            return NotFound();
        notification.MarkRead();
        await uow.SaveChangesAsync(ct);
        return NoContent();
    }

    [HttpPatch("read-all")]
    public async Task<IActionResult> MarkAllRead(CancellationToken ct = default)
    {
        await uow.Users.MarkAllNotificationsReadAsync(currentUser.UserId, ct);
        await uow.SaveChangesAsync(ct);
        return NoContent();
    }
}
