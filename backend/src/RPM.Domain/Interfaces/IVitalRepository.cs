using RPM.Domain.Entities;
namespace RPM.Domain.Interfaces;
public interface IVitalRepository
{
    Task<VitalRecord?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<IEnumerable<VitalRecord>> GetByPatientIdAsync(Guid patientId, DateTime from, DateTime to, int page, int pageSize, CancellationToken ct = default);
    Task<VitalRecord?> GetLatestByPatientIdAsync(Guid patientId, CancellationToken ct = default);
    Task<long> CountByPatientIdAsync(Guid patientId, DateTime from, DateTime to, CancellationToken ct = default);
    Task AddAsync(VitalRecord record, CancellationToken ct = default);
}
