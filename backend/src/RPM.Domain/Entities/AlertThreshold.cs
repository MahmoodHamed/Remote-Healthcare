using RPM.Domain.Common;
namespace RPM.Domain.Entities;

/// <summary>
/// Per-patient thresholds. Defaults are based on common adult clinical ranges
/// and are tunable by an attending doctor.
/// </summary>
public class AlertThreshold : BaseEntity
{
    public Guid PatientId { get; private set; }
    public PatientProfile Patient { get; private set; } = null!;

    public float MinHeartRate { get; private set; } = 40;
    public float MaxHeartRate { get; private set; } = 130;
    public float MinSpO2 { get; private set; } = 90;
    public float MaxSystolicBp { get; private set; } = 160;
    public float MaxDiastolicBp { get; private set; } = 100;
    public float MaxTemperatureC { get; private set; } = 38.5f;
    public float MaxSkinTemperatureC { get; private set; } = 38.0f;
    public float MinRespirationRate { get; private set; } = 8;
    public float MaxRespirationRate { get; private set; } = 24;
    public float MaxStressScore { get; private set; } = 80;
    public float MinBloodGlucoseMgDl { get; private set; } = 70;
    public float MaxBloodGlucoseMgDl { get; private set; } = 180;

    protected AlertThreshold() { }

    public static AlertThreshold CreateDefault(Guid patientId) => new() { PatientId = patientId };

    public void Update(float minHr, float maxHr, float minSpo2,
        float maxSysBp, float maxDiaBp, float maxTemp)
    {
        MinHeartRate = minHr;
        MaxHeartRate = maxHr;
        MinSpO2 = minSpo2;
        MaxSystolicBp = maxSysBp;
        MaxDiastolicBp = maxDiaBp;
        MaxTemperatureC = maxTemp;
        SetUpdatedAt();
    }

    public void UpdateAdvanced(float maxSkinTemp, float minRespRate, float maxRespRate,
        float maxStressScore, float minGlucose, float maxGlucose)
    {
        MaxSkinTemperatureC = maxSkinTemp;
        MinRespirationRate = minRespRate;
        MaxRespirationRate = maxRespRate;
        MaxStressScore = maxStressScore;
        MinBloodGlucoseMgDl = minGlucose;
        MaxBloodGlucoseMgDl = maxGlucose;
        SetUpdatedAt();
    }
}
