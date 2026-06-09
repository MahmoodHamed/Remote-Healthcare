namespace RPM.Application.DTOs.Patients;
public record PatientSummaryDto(
    Guid UserId, string FullName, string Email, string? AvatarUrl,
    DateOnly? DateOfBirth, string? BloodType, string? ShortPatientCode,
    VitalRecordLatestDto? LatestVitals = null);
public record PatientDetailDto(Guid UserId, string FullName, string Email, string Phone, string? AvatarUrl,
    DateOnly? DateOfBirth, string? BloodType, float? WeightKg, float? HeightCm,
    List<string> ChronicDiseases, List<string> Allergies, List<string> CurrentMedications,
    string? EmergencyContactPhone, string? ShortPatientCode, VitalRecordLatestDto? LatestVitals);
public record VitalRecordLatestDto(
    float? HeartRateBpm, float? SpO2Percent,
    float? SystolicBp, float? DiastolicBp,
    float? TemperatureC, float? SkinTemperatureC, float? AmbientTemperatureC,
    float? HrvMs, float? StressScore, float? BodyFatPercent, float? EcgAvgHeartRateBpm,
    int? StepsCount, float? CaloriesBurned,
    bool FallDetected, bool IsWearing,
    DateTime RecordedAt);
public record DoctorDto(Guid UserId, string FullName, string Specialization, string? HospitalName, string? AvatarUrl);
