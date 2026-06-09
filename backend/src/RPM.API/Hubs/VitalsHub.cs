using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
namespace RPM.API.Hubs;

[Authorize]
public class VitalsHub : Hub
{
    /// <summary>Client subscribes to a patient's real-time vitals</summary>
    public async Task SubscribeToPatient(string patientId) =>
        await Groups.AddToGroupAsync(Context.ConnectionId, $"vitals-{patientId}");

    public async Task UnsubscribeFromPatient(string patientId) =>
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"vitals-{patientId}");

    public override Task OnConnectedAsync()
    {
        var userId = Context.UserIdentifier;
        return base.OnConnectedAsync();
    }
}
