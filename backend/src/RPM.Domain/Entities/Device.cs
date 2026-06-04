using RPM.Domain.Common;
using RPM.Domain.Enums;
namespace RPM.Domain.Entities;
public class Device : BaseEntity
{
    public Guid PatientId { get; private set; }
    public PatientProfile Patient { get; private set; } = null!;
    public string DeviceName { get; private set; } = string.Empty;
    public string DeviceModel { get; private set; } = string.Empty;
    public string MqttClientId { get; private set; } = string.Empty;
    public string? FirmwareVersion { get; private set; }
    public DeviceStatus Status { get; private set; } = DeviceStatus.Offline;
    public float? BatteryLevel { get; private set; }
    public DateTime? LastSeenAt { get; private set; }
    public DateTime RegisteredAt { get; private set; } = DateTime.UtcNow;

    public ICollection<VitalRecord> VitalRecords { get; private set; } = [];

    protected Device() { }

    public static Device Create(Guid patientId, string name, string model, string mqttClientId)
        => new() { PatientId = patientId, DeviceName = name, DeviceModel = model, MqttClientId = mqttClientId };

    /// <summary>Creates a device with a pre-assigned ID (e.g. from MQTT vitals ingestion).</summary>
    public static Device CreateWithId(Guid id, Guid patientId, string name, string model, string mqttClientId)
    {
        var device = Create(patientId, name, model, mqttClientId);
        device.AssignId(id);
        return device;
    }

    public void UpdateStatus(DeviceStatus status, float? battery = null)
    {
        Status = status;
        BatteryLevel = battery;
        LastSeenAt = DateTime.UtcNow;
        SetUpdatedAt();
    }
}
