using RPM.Domain.Common;
namespace RPM.Domain.Entities;
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

    protected AlertThreshold() { }

    public static AlertThreshold CreateDefault(Guid patientId) => new() { PatientId = patientId };

    public void Update(float minHr, float maxHr, float minSpo2,
        float maxSysBp, float maxDiaBp, float maxTemp)
    {
        MinHeartRate = minHr; MaxHeartRate = maxHr;
        MinSpO2 = minSpo2; MaxSystolicBp = maxSysBp;
        MaxDiastolicBp = maxDiaBp; MaxTemperatureC = maxTemp;
        SetUpdatedAt();
    }
}
