using RPM.Application.DTOs.Chat;

namespace RPM.Application.Common.Interfaces;

public interface IChatHubService
{
    Task BroadcastMessageAsync(MessageDto message, CancellationToken ct = default);
}
