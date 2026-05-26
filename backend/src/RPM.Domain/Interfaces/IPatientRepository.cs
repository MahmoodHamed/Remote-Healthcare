using RPM.Domain.Entities;
namespace RPM.Domain.Interfaces;
public interface IPatientRepository
{
    Task<PatientProfile?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<PatientProfile?> GetByUserIdAsync(Guid userId, CancellationToken ct = default);
    Task<PatientProfile?> GetByPatientUserIdAsync(Guid userId, CancellationToken ct = default);
    Task<IEnumerable<PatientProfile>> GetByDoctorIdAsync(Guid doctorId, CancellationToken ct = default);
    Task<DoctorProfile?> GetDoctorProfileByUserIdAsync(Guid userId, CancellationToken ct = default);
    Task<IReadOnlyList<DoctorProfile>> GetAllDoctorsAsync(CancellationToken ct = default);
    Task<IReadOnlyList<PatientProfile>> GetAllPatientsAsync(CancellationToken ct = default);
    Task<DoctorPatientAssignment?> GetAssignmentAsync(Guid doctorUserId, Guid patientProfileId, CancellationToken ct = default);
    void UpdateAssignment(DoctorPatientAssignment assignment);
    Task AddPatientProfileAsync(PatientProfile profile, CancellationToken ct = default);
    Task AddDoctorProfileAsync(DoctorProfile profile, CancellationToken ct = default);
    Task AddAssignmentAsync(DoctorPatientAssignment assignment, CancellationToken ct = default);
    Task AddRelativeLinkAsync(PatientRelativeLink link, CancellationToken ct = default);
    void Update(PatientProfile profile);
}
