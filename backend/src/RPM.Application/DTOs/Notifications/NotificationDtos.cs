namespace RPM.Application.DTOs.Notifications;

public record NotificationDto(
    Guid Id,
    string Title,
    string Body,
    bool IsRead,
    DateTime SentAt,
    Guid? AlertId,
    string? DataPayload);

public record NotificationsPagedDto(
    IReadOnlyList<NotificationDto> Items,
    int UnreadCount,
    int Page,
    int PageSize);
