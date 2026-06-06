using Microsoft.Extensions.Configuration;
using RPM.Application.Common.Interfaces;
using StackExchange.Redis;

namespace RPM.Infrastructure.Services;

public class ChatPresenceService : IChatPresenceService
{
    private readonly IDatabase _db;

    public ChatPresenceService(IConfiguration config)
    {
        var connStr = config.GetConnectionString("Redis") ?? "localhost:6379";
        _db = ConnectionMultiplexer.Connect(connStr).GetDatabase();
    }

    public async Task JoinConversationAsync(Guid userId, Guid conversationId, string connectionId, CancellationToken ct = default)
    {
        await _db.HashIncrementAsync(ViewersKey(conversationId), userId.ToString());
        await _db.StringSetAsync(ConnectionKey(connectionId), $"{userId}|{conversationId}");
    }

    public async Task LeaveConversationAsync(string connectionId, CancellationToken ct = default)
    {
        var mapping = await _db.StringGetAsync(ConnectionKey(connectionId));
        if (mapping.IsNullOrEmpty) return;

        await _db.KeyDeleteAsync(ConnectionKey(connectionId));

        var parts = ((string)mapping!).Split('|', 2);
        if (parts.Length != 2 || !Guid.TryParse(parts[1], out var conversationId)) return;

        var remaining = await _db.HashDecrementAsync(ViewersKey(conversationId), parts[0]);
        if (remaining <= 0)
            await _db.HashDeleteAsync(ViewersKey(conversationId), parts[0]);
    }

    public async Task<bool> IsUserViewingAsync(Guid userId, Guid conversationId, CancellationToken ct = default)
    {
        var count = await _db.HashGetAsync(ViewersKey(conversationId), userId.ToString());
        return count.HasValue && (long)count > 0;
    }

    private static string ViewersKey(Guid conversationId) => $"chat:viewers:{conversationId}";
    private static string ConnectionKey(string connectionId) => $"chat:conn:{connectionId}";
}
