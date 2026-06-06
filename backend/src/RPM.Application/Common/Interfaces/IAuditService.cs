namespace RPM.Application.Common.Interfaces;

public record AuditLogEntry(
    Guid? UserId,
    string? UserEmail,
    string Action,
    string? Resource = null,
    string? Detail = null,
    string? IpAddress = null);

public interface IAuditService
{
    Task LogAsync(AuditLogEntry entry, CancellationToken ct = default);
    Task<IEnumerable<AuditLogRow>> GetRecentAsync(int page = 1, int pageSize = 50, CancellationToken ct = default);
}

public record AuditLogRow(
    Guid Id,
    Guid? UserId,
    string? UserEmail,
    string Action,
    string? Resource,
    string? Detail,
    string? IpAddress,
    DateTime OccurredAt);
