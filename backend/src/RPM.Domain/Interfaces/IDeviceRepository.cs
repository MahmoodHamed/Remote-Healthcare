using RPM.Domain.Entities;
namespace RPM.Domain.Interfaces;
public interface IDeviceRepository
{
    Task<Device?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<Device?> GetByMqttClientIdAsync(string mqttClientId, CancellationToken ct = default);
    Task<IEnumerable<Device>> GetByPatientIdAsync(Guid patientId, CancellationToken ct = default);
    Task AddAsync(Device device, CancellationToken ct = default);
    void Update(Device device);
}
