using Microsoft.EntityFrameworkCore;
using RPM.Application.Common.Interfaces;
using RPM.Infrastructure.Persistence;

namespace RPM.Infrastructure.Services;

public class AuditService(AppDbContext db) : IAuditService
{
    public async Task LogAsync(AuditLogEntry entry, CancellationToken ct = default)
    {
        await db.Database.ExecuteSqlRawAsync(
            """
            INSERT INTO "AuditLogs" ("Id","UserId","UserEmail","Action","Resource","Detail","IpAddress","OccurredAt")
            VALUES ({0},{1},{2},{3},{4},{5},{6},{7})
            """,
            [Guid.NewGuid(), entry.UserId, entry.UserEmail, entry.Action,
             entry.Resource, entry.Detail, entry.IpAddress, DateTime.UtcNow],
            ct);
    }

    public async Task<IEnumerable<AuditLogRow>> GetRecentAsync(int page = 1, int pageSize = 50, CancellationToken ct = default)
    {
        var offset = (page - 1) * pageSize;
        var rows = await db.Database.SqlQueryRaw<AuditLogRow>(
            """
            SELECT "Id","UserId","UserEmail","Action","Resource","Detail","IpAddress","OccurredAt"
            FROM "AuditLogs"
            ORDER BY "OccurredAt" DESC
            LIMIT {0} OFFSET {1}
            """,
            pageSize, offset)
            .ToListAsync(ct);
        return rows;
    }
}
