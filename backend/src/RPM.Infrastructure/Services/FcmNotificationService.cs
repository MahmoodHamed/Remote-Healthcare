using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Google.Apis.Auth.OAuth2;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using RPM.Application.Common.Interfaces;

namespace RPM.Infrastructure.Services;

public class FcmNotificationService : INotificationService
{
    private readonly IConfiguration _config;
    private readonly ILogger<FcmNotificationService> _logger;
    private bool _initAttempted;
    private bool _enabled;

    public FcmNotificationService(IConfiguration config, ILogger<FcmNotificationService> logger)
    {
        _config = config;
        _logger = logger;
    }

    public async Task SendPushAsync(string fcmToken, string title, string body,
        Dictionary<string, string>? data = null, string channelId = "rpm_alerts", CancellationToken ct = default)
    {
        if (!EnsureInitialized() || string.IsNullOrWhiteSpace(fcmToken)) return;

        var msg = new Message
        {
            Token = fcmToken,
            Notification = new Notification { Title = title, Body = body },
            Data = data,
            Android = BuildAndroidConfig(channelId)
        };
        await FirebaseMessaging.DefaultInstance.SendAsync(msg, ct);
    }

    public async Task SendPushToManyAsync(IEnumerable<string> fcmTokens, string title, string body,
        Dictionary<string, string>? data = null, string channelId = "rpm_alerts", CancellationToken ct = default)
    {
        if (!EnsureInitialized()) return;

        var tokens = fcmTokens.Where(t => !string.IsNullOrWhiteSpace(t)).Distinct().ToList();
        if (tokens.Count == 0) return;

        var multicast = new MulticastMessage
        {
            Tokens = tokens,
            Notification = new Notification { Title = title, Body = body },
            Data = data,
            Android = BuildAndroidConfig(channelId)
        };
        await FirebaseMessaging.DefaultInstance.SendEachForMulticastAsync(multicast, ct);
    }

    private bool EnsureInitialized()
    {
        if (_initAttempted) return _enabled;
        _initAttempted = true;

        if (FirebaseApp.DefaultInstance is not null)
        {
            _enabled = true;
            return true;
        }

        var credPath = _config["Firebase:CredentialPath"];
        if (string.IsNullOrWhiteSpace(credPath) || !File.Exists(credPath))
        {
            _logger.LogWarning(
                "Firebase credentials not found at {Path}. Push notifications are disabled.",
                string.IsNullOrWhiteSpace(credPath) ? "(not configured)" : credPath);
            return false;
        }

        try
        {
            FirebaseApp.Create(new AppOptions { Credential = GoogleCredential.FromFile(credPath) });
            _enabled = true;
            _logger.LogInformation("Firebase push notifications enabled.");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to initialize Firebase. Push notifications are disabled.");
        }

        return _enabled;
    }

    private static AndroidConfig BuildAndroidConfig(string channelId) => new()
    {
        Priority = Priority.High,
        Notification = new AndroidNotification
        {
            Sound = "default",
            ChannelId = channelId,
            DefaultSound = true,
            Priority = NotificationPriority.HIGH
        }
    };
}
