namespace RPM.Application.DTOs.Alerts;
public record AlertDto(Guid Id, Guid PatientId, string Type, string Severity, string Message, string Status, DateTime TriggeredAt, DateTime? ResolvedAt);
public record AlertPagedDto(IEnumerable<AlertDto> Items, long TotalCount, int Page, int PageSize);
