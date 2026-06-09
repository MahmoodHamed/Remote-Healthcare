namespace RPM.Application.Common.Interfaces;
public interface INotificationService
{
    Task SendPushAsync(string fcmToken, string title, string body,
        Dictionary<string, string>? data = null, string channelId = "rpm_alerts", CancellationToken ct = default);
    Task SendPushToManyAsync(IEnumerable<string> fcmTokens, string title, string body,
        Dictionary<string, string>? data = null, string channelId = "rpm_alerts", CancellationToken ct = default);
}
