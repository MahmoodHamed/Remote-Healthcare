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
        Dictionary<string, string>? data = null, CancellationToken ct = default)
    {
        var msg = new Message
        {
            Token = fcmToken,
            Notification = new Notification { Title = title, Body = body },
            Data = data,
            Android = new AndroidConfig
            {
                Priority = Priority.High,
                Notification = new AndroidNotification { Sound = "default", ChannelId = "rpm_alerts" }
            }
        };
        await FirebaseMessaging.DefaultInstance.SendAsync(msg, ct);
    }

    public async Task SendPushToManyAsync(IEnumerable<string> fcmTokens, string title, string body,
        Dictionary<string, string>? data = null, CancellationToken ct = default)
    {
        var tokens = fcmTokens.Distinct().ToList();
        if (tokens.Count == 0) return;

        var multicast = new MulticastMessage
        {
            Tokens = tokens,
            Notification = new Notification { Title = title, Body = body },
            Data = data,
            Android = new AndroidConfig
            {
                Priority = Priority.High,
                Notification = new AndroidNotification { Sound = "default", ChannelId = "rpm_alerts" }
            }
        };
        await FirebaseMessaging.DefaultInstance.SendEachForMulticastAsync(multicast, ct);
    }
}
