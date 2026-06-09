using System.Text.Json.Serialization;
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
    bool IsActive,
    VitalRecordLatestDto? LatestVitals = null);

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
    IReadOnlyList<DoctorAssignmentDto> Doctors,
    string? WatchShortId = null,
    DoctorSimpleDto? Doctor = null);

public record SetWatchShortIdRequest(string? ShortId);

/// <summary>Full latest vitals snapshot returned inside patient profile — aligned with mobile client fields.</summary>
public record VitalRecordLatestDto(
    float? HeartRateBpm,
    float? SpO2Percent,
    float? SystolicBp,
    float? DiastolicBp,
    float? TemperatureC,
    float? SkinTemperatureC,
    [property: JsonPropertyName("ambientTemperatureC")] float? AmbientTemperatureC,
    [property: JsonPropertyName("hrvMs")] float? HeartRateVariabilityMs,
    float? StressScore,
    float? BodyFatPercent,
    [property: JsonPropertyName("ecgAvgHeartRateBpm")] float? EcgAverageHeartRate,
    int? StepsCount,
    float? CaloriesBurned,
    bool FallDetected,
    bool IsWearing,
    DateTime RecordedAt);

/// <summary>Primary doctor for mobile display — simplified single-doctor view.</summary>
public record DoctorSimpleDto(
    Guid UserId,
    string FullName,
    string? Specialization);

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
