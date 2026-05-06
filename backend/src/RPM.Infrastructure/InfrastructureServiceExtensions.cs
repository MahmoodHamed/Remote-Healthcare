using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using RPM.Application.Common.Interfaces;
using RPM.Domain.Interfaces;
using RPM.Infrastructure.BackgroundServices;
using RPM.Infrastructure.Persistence;
using RPM.Infrastructure.Services;
namespace RPM.Infrastructure;

public static class InfrastructureServiceExtensions
{
    public static IServiceCollection AddInfrastructure(this IServiceCollection services, IConfiguration config)
    {
        // Database
        services.AddDbContext<AppDbContext>(options =>
            options.UseNpgsql(config.GetConnectionString("DefaultConnection"),
                npgsql => npgsql.EnableRetryOnFailure(3)));

        services.AddScoped<IUnitOfWork, UnitOfWork>();

        // Auth Services
        services.AddScoped<IJwtService, JwtService>();
        services.AddScoped<IPasswordHasher, PasswordHasher>();

        // Cache
        services.AddSingleton<ICacheService, RedisCacheService>();

        // External Services
        services.AddSingleton<INotificationService, FcmNotificationService>();
        services.AddSingleton<IStorageService, MinioStorageService>();

        // HttpContext
        services.AddHttpContextAccessor();
        services.AddScoped<ICurrentUser, CurrentUserService>();

        // MQTT Background Service
        services.AddHostedService<MqttBackgroundService>();

        // SignalR Redis Backplane
        services.AddSignalR()
            .AddStackExchangeRedis(config.GetConnectionString("Redis") ?? "localhost:6379");

        return services;
    }
}
