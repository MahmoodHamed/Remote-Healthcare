namespace RPM.Application.DTOs.Devices;

public record DeviceDto(
    Guid Id,
    string DeviceName,
    string DeviceModel,
    string Status,
    float? BatteryLevel,
    DateTime? LastSeenAt,
    DateTime RegisteredAt);

public record PairingInfoDto(
    string PatientId,
    string MqttHost,
    int MqttPort);

public record RenameDeviceRequest(string NewName);
