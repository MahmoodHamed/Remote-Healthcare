using RPM.Domain.Entities;
namespace RPM.Domain.Interfaces;
public interface IUserRepository
{
    Task<User?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<User?> GetByEmailAsync(string email, CancellationToken ct = default);
    Task<bool> ExistsByEmailAsync(string email, CancellationToken ct = default);
    Task AddAsync(User user, CancellationToken ct = default);
    Task AddNotificationAsync(Notification notification, CancellationToken ct = default);
    void Update(User user);
}
