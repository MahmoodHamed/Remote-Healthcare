using MediatR;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Devices;
using RPM.Domain.Interfaces;

namespace RPM.Application.Features.Devices;

public class GetMyDevicesHandler(IUnitOfWork uow, ICurrentUser currentUser)
    : IRequestHandler<GetMyDevicesQuery, IEnumerable<DeviceDto>>
{
    public async Task<IEnumerable<DeviceDto>> Handle(GetMyDevicesQuery _, CancellationToken ct)
    {
        var profile = await uow.Patients.GetByUserIdAsync(currentUser.UserId, ct);
        if (profile is null) return [];

        var devices = await uow.Devices.GetByPatientIdAsync(profile.Id, ct);
        return devices.Select(d => new DeviceDto(
            d.Id, d.DeviceName, d.DeviceModel,
            d.Status.ToString(), d.BatteryLevel, d.LastSeenAt, d.RegisteredAt));
    }
}

public class GetPatientDevicesHandler(IUnitOfWork uow)
    : IRequestHandler<GetPatientDevicesQuery, IEnumerable<DeviceDto>>
{
    public async Task<IEnumerable<DeviceDto>> Handle(GetPatientDevicesQuery query, CancellationToken ct)
    {
        var profile = await uow.Patients.GetByUserIdAsync(query.PatientUserId, ct);
        if (profile is null) return [];

        var devices = await uow.Devices.GetByPatientIdAsync(profile.Id, ct);
        return devices.Select(d => new DeviceDto(
            d.Id, d.DeviceName, d.DeviceModel,
            d.Status.ToString(), d.BatteryLevel, d.LastSeenAt, d.RegisteredAt));
    }
}

public class GetPairingInfoHandler(ICurrentUser currentUser, IMqttBrokerSettings mqtt)
    : IRequestHandler<GetPairingInfoQuery, PairingInfoDto>
{
    public Task<PairingInfoDto> Handle(GetPairingInfoQuery _, CancellationToken ct)
    {
        // PatientId is the User's GUID — the watch sends this in the MQTT topic
        // and MqttBackgroundService.NormalizeGuid handles full GUIDs directly.
        var patientId = currentUser.UserId.ToString();
        return Task.FromResult(new PairingInfoDto(patientId, mqtt.PublicHost, mqtt.Port));
    }
}

public class RenameDeviceHandler(IUnitOfWork uow, ICurrentUser currentUser)
    : IRequestHandler<RenameDeviceCommand>
{
    public async Task Handle(RenameDeviceCommand cmd, CancellationToken ct)
    {
        var device = await uow.Devices.GetByIdAsync(cmd.DeviceId, ct)
            ?? throw new KeyNotFoundException($"Device {cmd.DeviceId} not found.");

        // Only allow the device's patient (or admin) to rename it
        var profile = await uow.Patients.GetByUserIdAsync(currentUser.UserId, ct);
        if (profile is null || (device.PatientId != profile.Id && currentUser.Role != "Admin"))
            throw new UnauthorizedAccessException("Not authorized to rename this device.");

        device.UpdateName(cmd.NewName);
        uow.Devices.Update(device);
        await uow.SaveChangesAsync(ct);
    }
}
