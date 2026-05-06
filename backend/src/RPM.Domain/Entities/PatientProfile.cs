using RPM.Domain.Common;
using RPM.Domain.Enums;
namespace RPM.Domain.Entities;
public class PatientProfile : BaseEntity
{
    public Guid UserId { get; private set; }
    public User User { get; private set; } = null!;
    public DateOnly? DateOfBirth { get; private set; }
    public BloodType? BloodType { get; private set; }
    public float? WeightKg { get; private set; }
    public float? HeightCm { get; private set; }
    public List<string> ChronicDiseases { get; private set; } = [];
    public List<string> Allergies { get; private set; } = [];
    public List<string> CurrentMedications { get; private set; } = [];
    public string? EmergencyContactPhone { get; private set; }

    public AlertThreshold? AlertThreshold { get; private set; }
    public ICollection<Device> Devices { get; private set; } = [];
    public ICollection<VitalRecord> VitalRecords { get; private set; } = [];
    public ICollection<DoctorPatientAssignment> DoctorAssignments { get; private set; } = [];
    public ICollection<PatientRelativeLink> RelativeLinks { get; private set; } = [];

    protected PatientProfile() { }

    public static PatientProfile Create(Guid userId) => new() { UserId = userId };

    public void UpdateMedicalInfo(DateOnly? dob, BloodType? blood, float? weight, float? height,
        List<string>? diseases, List<string>? allergies, List<string>? meds, string? emergencyPhone)
    {
        DateOfBirth = dob;
        BloodType = blood;
        WeightKg = weight;
        HeightCm = height;
        ChronicDiseases = diseases ?? [];
        Allergies = allergies ?? [];
        CurrentMedications = meds ?? [];
        EmergencyContactPhone = emergencyPhone;
        SetUpdatedAt();
    }
}
