using System.Text.Json;
using Microsoft.Extensions.Configuration;
using RPM.Application.Common.Interfaces;
using StackExchange.Redis;
namespace RPM.Infrastructure.Services;

public class RedisCacheService : ICacheService
{
    private readonly IDatabase _db;

    public RedisCacheService(IConfiguration config)
    {
        var connStr = config.GetConnectionString("Redis") ?? "localhost:6379";
        var redis = ConnectionMultiplexer.Connect(connStr);
        _db = redis.GetDatabase();
    }

    public async Task<T?> GetAsync<T>(string key, CancellationToken ct = default)
    {
        var val = await _db.StringGetAsync(key);
        return val.IsNullOrEmpty ? default : JsonSerializer.Deserialize<T>((string)val!);
    }

    public async Task SetAsync<T>(string key, T value, TimeSpan? expiry = null, CancellationToken ct = default)
    {
        var json = JsonSerializer.Serialize(value);
        if (expiry.HasValue)
            await _db.StringSetAsync(key, json, expiry.Value);
        else
            await _db.StringSetAsync(key, json);
    }

    public Task RemoveAsync(string key, CancellationToken ct = default) =>
        _db.KeyDeleteAsync(key).ContinueWith(_ => { }, ct);

    public Task<bool> ExistsAsync(string key, CancellationToken ct = default) =>
        _db.KeyExistsAsync(key);
}
