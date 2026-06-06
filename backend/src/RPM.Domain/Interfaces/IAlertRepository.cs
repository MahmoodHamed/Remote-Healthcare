using RPM.Domain.Entities;
using RPM.Domain.Enums;
namespace RPM.Domain.Interfaces;
public interface IAlertRepository
{
    Task<Alert?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<IEnumerable<Alert>> GetByPatientIdAsync(Guid patientId, int page, int pageSize, CancellationToken ct = default);
    Task<IEnumerable<Alert>> GetUnresolvedByPatientIdAsync(Guid patientId, CancellationToken ct = default);
    /// <summary>Looks up threshold using PatientProfile.Id (the FK stored on AlertThreshold).</summary>
    Task<AlertThreshold?> GetThresholdByPatientIdAsync(Guid patientProfileId, CancellationToken ct = default);
    /// <summary>Looks up threshold via User.Id → PatientProfile.Id join. Use this from vital event handlers.</summary>
    Task<AlertThreshold?> GetThresholdByUserIdAsync(Guid userId, CancellationToken ct = default);
    /// <summary>Returns the most recent unresolved alert of the given type within the lookback window.</summary>
    Task<Alert?> GetRecentAlertAsync(Guid patientId, AlertType type, TimeSpan lookback, CancellationToken ct = default);
    Task AddAsync(Alert alert, CancellationToken ct = default);
    Task AddThresholdAsync(AlertThreshold threshold, CancellationToken ct = default);
    void Update(Alert alert);
    void UpdateThreshold(AlertThreshold threshold);
}
