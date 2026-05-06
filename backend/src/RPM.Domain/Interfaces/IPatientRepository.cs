using RPM.Domain.Entities;
namespace RPM.Domain.Interfaces;
public interface IPatientRepository
{
    Task<PatientProfile?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<PatientProfile?> GetByUserIdAsync(Guid userId, CancellationToken ct = default);
    Task<PatientProfile?> GetByPatientUserIdAsync(Guid userId, CancellationToken ct = default);
    Task<IEnumerable<PatientProfile>> GetByDoctorIdAsync(Guid doctorId, CancellationToken ct = default);
    Task<DoctorProfile?> GetDoctorProfileByUserIdAsync(Guid userId, CancellationToken ct = default);
    Task AddPatientProfileAsync(PatientProfile profile, CancellationToken ct = default);
    Task AddDoctorProfileAsync(DoctorProfile profile, CancellationToken ct = default);
    Task AddAssignmentAsync(DoctorPatientAssignment assignment, CancellationToken ct = default);
    Task AddRelativeLinkAsync(PatientRelativeLink link, CancellationToken ct = default);
    void Update(PatientProfile profile);
}
