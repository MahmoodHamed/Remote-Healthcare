using RPM.Domain.Interfaces;

namespace RPM.API.Helpers;

/// <summary>
/// Maps a patient's account UUID to the streaming UUID used by the watch (derived from WatchShortId).
/// </summary>
public interface IVitalsPatientIdResolver
{
    Task<Guid> ResolveAsync(Guid patientId, CancellationToken ct = default);
}

public class VitalsPatientIdResolver(IUnitOfWork uow) : IVitalsPatientIdResolver
{
    public async Task<Guid> ResolveAsync(Guid patientId, CancellationToken ct = default)
    {
        var profile = await uow.Patients.GetByUserIdAsync(patientId, ct);
        if (profile?.WatchShortId is { Length: 6 } shortId)
        {
            var streamingId = PatientIdNormalizer.ToGuid(shortId);
            if (streamingId is not null) return streamingId.Value;
        }

        return patientId;
    }
}
