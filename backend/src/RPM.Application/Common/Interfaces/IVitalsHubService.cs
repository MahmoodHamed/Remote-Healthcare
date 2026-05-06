namespace RPM.Application.Common.Interfaces;
public interface IVitalsHubService
{
    Task BroadcastVitalsAsync(Guid patientId, object vitalsDto, CancellationToken ct = default);
    Task BroadcastAlertAsync(Guid patientId, object alertDto, CancellationToken ct = default);
}
