using Microsoft.AspNetCore.SignalR;
using RPM.API.Hubs;
using RPM.Application.Common.Interfaces;
namespace RPM.API.Extensions;

public class VitalsHubService(IHubContext<VitalsHub> hub) : IVitalsHubService
{
    public Task BroadcastVitalsAsync(Guid patientId, object vitalsDto, CancellationToken ct = default) =>
        hub.Clients.Group($"vitals-{patientId}").SendAsync("ReceiveVitals", vitalsDto, ct);

    public Task BroadcastAlertAsync(Guid patientId, object alertDto, CancellationToken ct = default) =>
        hub.Clients.Group($"vitals-{patientId}").SendAsync("ReceiveAlert", alertDto, ct);
}
