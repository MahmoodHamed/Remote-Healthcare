namespace RPM.Application.DTOs.Devices;

public record DeviceDto(
    Guid Id,
    string DeviceName,
    string DeviceModel,
    string Status,
    float? BatteryLevel,
    DateTime? LastSeenAt,
    DateTime RegisteredAt);

/// <param name="PatientId">6-character watch code (e.g. 7K7RSB) — enter on the watch.</param>
/// <param name="StreamingPatientId">Internal user GUID used by SignalR and REST after the short code is resolved.</param>
public record PairingInfoDto(
    string PatientId,
    string StreamingPatientId,
    string MqttHost,
    int MqttPort);

public record SavePairingInfoRequest(string PatientId);

public record RenameDeviceRequest(string NewName);
