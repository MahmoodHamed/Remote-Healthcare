using MediatR;
using RPM.Application.DTOs.Devices;

namespace RPM.Application.Features.Devices;

public record GetMyDevicesQuery : IRequest<IEnumerable<DeviceDto>>;

public record GetPatientDevicesQuery(Guid PatientUserId) : IRequest<IEnumerable<DeviceDto>>;

public record GetPairingInfoQuery : IRequest<PairingInfoDto>;

public record SavePairingInfoCommand(string PatientId) : IRequest<PairingInfoDto>;

public record RenameDeviceCommand(Guid DeviceId, string NewName) : IRequest;
