using RPM.Domain.Interfaces;
using RPM.Infrastructure.Persistence.Repositories;
namespace RPM.Infrastructure.Persistence;

public class UnitOfWork(AppDbContext db) : IUnitOfWork
{
    public IUserRepository Users { get; } = new UserRepository(db);
    public IVitalRepository Vitals { get; } = new VitalRepository(db);
    public IAlertRepository Alerts { get; } = new AlertRepository(db);
    public IChatRepository Chat { get; } = new ChatRepository(db);
    public IDeviceRepository Devices { get; } = new DeviceRepository(db);
    public IPatientRepository Patients { get; } = new PatientRepository(db);
    public Task<int> SaveChangesAsync(CancellationToken ct = default) => db.SaveChangesAsync(ct);
}
