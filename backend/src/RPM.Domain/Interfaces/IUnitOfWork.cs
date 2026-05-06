namespace RPM.Domain.Interfaces;
public interface IUnitOfWork
{
    IUserRepository Users { get; }
    IVitalRepository Vitals { get; }
    IAlertRepository Alerts { get; }
    IChatRepository Chat { get; }
    IDeviceRepository Devices { get; }
    IPatientRepository Patients { get; }
    Task<int> SaveChangesAsync(CancellationToken ct = default);
}
