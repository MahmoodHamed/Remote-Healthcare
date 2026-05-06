using RPM.Domain.Common;
using RPM.Domain.Enums;
namespace RPM.Domain.Entities;
public class DoctorPatientAssignment : BaseEntity
{
    public Guid DoctorId { get; private set; }
    public User Doctor { get; private set; } = null!;
    public Guid PatientId { get; private set; }
    public PatientProfile Patient { get; private set; } = null!;
    public RelationshipAssignmentStatus Status { get; private set; } = RelationshipAssignmentStatus.Pending;
    public DateTime AssignedAt { get; private set; } = DateTime.UtcNow;

    protected DoctorPatientAssignment() { }

    public static DoctorPatientAssignment Create(Guid doctorId, Guid patientId)
        => new() { DoctorId = doctorId, PatientId = patientId };

    public void Activate() { Status = RelationshipAssignmentStatus.Active; SetUpdatedAt(); }
    public void Deactivate() { Status = RelationshipAssignmentStatus.Inactive; SetUpdatedAt(); }
}
