using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using RPM.API.Helpers;
namespace RPM.API.Hubs;

[Authorize]
public class VitalsHub(IVitalsPatientIdResolver patientIdResolver) : Hub
{
    /// <summary>Client subscribes to a patient's real-time vitals</summary>
    public async Task SubscribeToPatient(string patientId)
    {
        var normalized = PatientIdNormalizer.ToGuid(patientId);
        if (normalized is null) throw new HubException("Invalid patient ID.");
        var streamingId = await patientIdResolver.ResolveAsync(normalized.Value);
        await Groups.AddToGroupAsync(Context.ConnectionId, PatientIdNormalizer.VitalsGroupName(streamingId));
    }

    public async Task UnsubscribeFromPatient(string patientId)
    {
        var normalized = PatientIdNormalizer.ToGuid(patientId);
        if (normalized is null) throw new HubException("Invalid patient ID.");
        var streamingId = await patientIdResolver.ResolveAsync(normalized.Value);
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, PatientIdNormalizer.VitalsGroupName(streamingId));
    }

    public override Task OnConnectedAsync() => base.OnConnectedAsync();
}
