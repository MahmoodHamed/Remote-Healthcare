namespace RPM.Application.DTOs.Chat;
public record ConversationDto(Guid Id, string Type, string? Name, DateTime? LastMessageAt, List<ParticipantDto> Participants);
public record ParticipantDto(Guid UserId, string FullName, string? AvatarUrl, bool IsAdmin);
public record MessageDto(Guid Id, Guid ConversationId, Guid SenderId, string SenderName, string Content, string Type, string? MediaUrl, bool IsDeleted, DateTime SentAt);
public record MessagePagedDto(IEnumerable<MessageDto> Items, long TotalCount, int Page, int PageSize);
