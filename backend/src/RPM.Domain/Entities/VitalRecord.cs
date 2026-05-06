using RPM.Domain.Common;
using RPM.Domain.Events;
namespace RPM.Domain.Entities;
public class VitalRecord : BaseEntity
{
    public Guid PatientId { get; private set; }
    public User Patient { get; private set; } = null!;
    public Guid DeviceId { get; private set; }
    public Device Device { get; private set; } = null!;
    public float? HeartRateBpm { get; private set; }
    public float? SpO2Percent { get; private set; }
    public float? SystolicBp { get; private set; }
    public float? DiastolicBp { get; private set; }
    public float? TemperatureC { get; private set; }
    public int? StepsCount { get; private set; }
    public float? CaloriesBurned { get; private set; }
    public bool FallDetected { get; private set; }
    public bool IsWearing { get; private set; } = true;
    public DateTime RecordedAt { get; private set; } = DateTime.UtcNow;

    public ICollection<Alert> Alerts { get; private set; } = [];

    protected VitalRecord() { }

    public static VitalRecord Create(Guid patientId, Guid deviceId,
        float? hr, float? spo2, float? sysBp, float? diaBp,
        float? temp, int? steps, float? cal, bool fall, bool wearing)
    {
        var record = new VitalRecord
        {
            PatientId = patientId,
            DeviceId = deviceId,
            HeartRateBpm = hr,
            SpO2Percent = spo2,
            SystolicBp = sysBp,
            DiastolicBp = diaBp,
            TemperatureC = temp,
            StepsCount = steps,
            CaloriesBurned = cal,
            FallDetected = fall,
            IsWearing = wearing,
            RecordedAt = DateTime.UtcNow
        };
        record.AddDomainEvent(new VitalRecordedEvent(record.Id, patientId, deviceId));
        return record;
    }
}
