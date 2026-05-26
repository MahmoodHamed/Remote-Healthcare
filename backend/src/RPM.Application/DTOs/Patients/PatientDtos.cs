namespace RPM.Application.DTOs.Patients;

public record PatientSummaryDto(
    Guid UserId,
    Guid ProfileId,
    string FullName,
    string Email,
    string? Phone,
    string? AvatarUrl,
    DateOnly? DateOfBirth,
    string? BloodType,
    bool IsActive);

public record PatientDetailDto(
    Guid UserId,
    Guid ProfileId,
    string FullName,
    string Email,
    string Phone,
    string? AvatarUrl,
    DateOnly? DateOfBirth,
    string? BloodType,
    float? WeightKg,
    float? HeightCm,
    List<string> ChronicDiseases,
    List<string> Allergies,
    List<string> CurrentMedications,
    string? EmergencyContactPhone,
    VitalRecordLatestDto? LatestVitals,
    IReadOnlyList<DoctorAssignmentDto> Doctors);

public record VitalRecordLatestDto(
    float? HeartRateBpm,
    float? SpO2Percent,
    float? SystolicBp,
    float? DiastolicBp,
    float? TemperatureC,
    DateTime RecordedAt);

public record DoctorDto(
    Guid UserId,
    string FullName,
    string Email,
    string? Phone,
    string Specialization,
    string? LicenseNumber,
    string? HospitalName,
    string? AvatarUrl,
    bool IsActive,
    int PatientCount);

public record DoctorAssignmentDto(
    Guid DoctorUserId,
    string DoctorName,
    string Specialization,
    string Status,
    DateTime AssignedAt);

public record DoctorWithPatientsDto(
    DoctorDto Doctor,
    IReadOnlyList<PatientSummaryDto> Patients);

public record AssignmentRequestDto(Guid DoctorUserId, Guid PatientUserId);

public record AdminOverviewDto(
    int TotalUsers,
    int TotalDoctors,
    int TotalPatients,
    int TotalRelatives,
    int ActiveAssignments,
    int UnreadNotifications,
    int OpenAlerts);
