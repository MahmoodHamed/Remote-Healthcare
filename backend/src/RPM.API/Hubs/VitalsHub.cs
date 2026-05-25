using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using RPM.API.Helpers;
namespace RPM.API.Hubs;

[Authorize]
public class VitalsHub : Hub
{
    /// <summary>Client subscribes to a patient's real-time vitals</summary>
    public async Task SubscribeToPatient(string patientId)
    {
        var normalized = PatientIdNormalizer.ToGuid(patientId);
        if (normalized is null) throw new HubException("Invalid patient ID.");
        await Groups.AddToGroupAsync(Context.ConnectionId, PatientIdNormalizer.VitalsGroupName(normalized.Value));
    }

    public async Task UnsubscribeFromPatient(string patientId)
    {
        var normalized = PatientIdNormalizer.ToGuid(patientId);
        if (normalized is null) throw new HubException("Invalid patient ID.");
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, PatientIdNormalizer.VitalsGroupName(normalized.Value));
    }

    public override Task OnConnectedAsync()
    {
        var userId = Context.UserIdentifier;
        return base.OnConnectedAsync();
    }
}
