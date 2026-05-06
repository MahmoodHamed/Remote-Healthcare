using RPM.Domain.Common;
namespace RPM.Domain.Entities;
public class DoctorProfile : BaseEntity
{
    public Guid UserId { get; private set; }
    public User User { get; private set; } = null!;
    public string Specialization { get; private set; } = string.Empty;
    public string LicenseNumber { get; private set; } = string.Empty;
    public string? HospitalName { get; private set; }
    public string? Bio { get; private set; }

    public ICollection<DoctorPatientAssignment> PatientAssignments { get; private set; } = [];

    protected DoctorProfile() { }

    public static DoctorProfile Create(Guid userId, string specialization, string licenseNumber,
        string? hospital = null, string? bio = null)
        => new()
        {
            UserId = userId,
            Specialization = specialization,
            LicenseNumber = licenseNumber,
            HospitalName = hospital,
            Bio = bio
        };

    public void Update(string specialization, string? hospital, string? bio)
    {
        Specialization = specialization;
        HospitalName = hospital;
        Bio = bio;
        SetUpdatedAt();
    }
}
