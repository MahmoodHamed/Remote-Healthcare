using RPM.Domain.Entities;
namespace RPM.Domain.Interfaces;
public interface IAlertRepository
{
    Task<Alert?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<IEnumerable<Alert>> GetByPatientIdAsync(Guid patientId, int page, int pageSize, CancellationToken ct = default);
    Task<IEnumerable<Alert>> GetUnresolvedByPatientIdAsync(Guid patientId, CancellationToken ct = default);
    Task<AlertThreshold?> GetThresholdByPatientIdAsync(Guid patientId, CancellationToken ct = default);
    Task AddAsync(Alert alert, CancellationToken ct = default);
    Task AddThresholdAsync(AlertThreshold threshold, CancellationToken ct = default);
    void Update(Alert alert);
    void UpdateThreshold(AlertThreshold threshold);
}
