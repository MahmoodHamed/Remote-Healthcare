using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Google.Apis.Auth.OAuth2;
using Microsoft.Extensions.Configuration;
using RPM.Application.Common.Interfaces;
namespace RPM.Infrastructure.Services;

public class FcmNotificationService : INotificationService
{
    public FcmNotificationService(IConfiguration config)
    {
        if (FirebaseApp.DefaultInstance is null)
        {
            var credPath = config["Firebase:CredentialPath"];
            FirebaseApp.Create(new AppOptions
            {
                Credential = credPath != null
                    ? GoogleCredential.FromFile(credPath)
                    : GoogleCredential.GetApplicationDefault()
            });
        }
    }

    public async Task SendPushAsync(string fcmToken, string title, string body,
        Dictionary<string, string>? data = null, bool dataOnly = false, CancellationToken ct = default)
    {
        await FirebaseMessaging.DefaultInstance.SendAsync(
            BuildMessage(fcmToken, title, body, data, dataOnly), ct);
    }

    public async Task SendPushToManyAsync(IEnumerable<string> fcmTokens, string title, string body,
        Dictionary<string, string>? data = null, bool dataOnly = false, CancellationToken ct = default)
    {
        var tokens = fcmTokens.Distinct().Where(t => !string.IsNullOrWhiteSpace(t)).ToList();
        if (tokens.Count == 0) return;

        var multicast = new MulticastMessage
        {
            Tokens = tokens,
            Data = BuildDataPayload(title, body, data),
            Android = new AndroidConfig { Priority = Priority.High },
        };

        if (!dataOnly)
        {
            multicast.Notification = new Notification { Title = title, Body = body };
            multicast.Android.Notification = new AndroidNotification
            {
                Sound = "default",
                ChannelId = data?.GetValueOrDefault("channelId") ?? "rpm_alerts",
            };
        }

        await FirebaseMessaging.DefaultInstance.SendEachForMulticastAsync(multicast, ct);
    }

    private static Message BuildMessage(string token, string title, string body,
        Dictionary<string, string>? data, bool dataOnly)
    {
        var msg = new Message
        {
            Token = token,
            Data = BuildDataPayload(title, body, data),
            Android = new AndroidConfig { Priority = Priority.High },
        };

        if (!dataOnly)
        {
            msg.Notification = new Notification { Title = title, Body = body };
            msg.Android.Notification = new AndroidNotification
            {
                Sound = "default",
                ChannelId = data?.GetValueOrDefault("channelId") ?? "rpm_alerts",
            };
        }

        return msg;
    }

    private static Dictionary<string, string> BuildDataPayload(string title, string body, Dictionary<string, string>? data)
    {
        var payload = new Dictionary<string, string>(StringComparer.Ordinal)
        {
            ["title"] = title,
            ["body"] = body,
        };
        if (data is null) return payload;
        foreach (var (key, value) in data)
            payload[key] = value;
        return payload;
    }
}
