using Microsoft.EntityFrameworkCore;
using RPM.Domain.Entities;
using RPM.Domain.Interfaces;
using RPM.Infrastructure.Persistence;
namespace RPM.Infrastructure.Persistence.Repositories;

public class UserRepository(AppDbContext db) : IUserRepository
{
    public Task<User?> GetByIdAsync(Guid id, CancellationToken ct = default) =>
        db.Users.FirstOrDefaultAsync(u => u.Id == id, ct);

    public Task<User?> GetByEmailAsync(string email, CancellationToken ct = default) =>
        db.Users.FirstOrDefaultAsync(u => u.Email == email.ToLowerInvariant(), ct);

    public async Task<IEnumerable<User>> GetAllAsync(CancellationToken ct = default) =>
        await db.Users.Include(u => u.RefreshTokens).OrderByDescending(u => u.CreatedAt).ToListAsync(ct);

    public Task<bool> ExistsByEmailAsync(string email, CancellationToken ct = default) =>
        db.Users.AnyAsync(u => u.Email == email.ToLowerInvariant(), ct);

    public async Task AddAsync(User user, CancellationToken ct = default) =>
        await db.Users.AddAsync(user, ct);

    public async Task AddNotificationAsync(Notification notification, CancellationToken ct = default) =>
        await db.Notifications.AddAsync(notification, ct);

    public async Task<IEnumerable<Notification>> GetNotificationsAsync(Guid userId, int page, int pageSize, CancellationToken ct = default) =>
        await db.Notifications
            .Where(n => n.UserId == userId)
            .OrderByDescending(n => n.SentAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync(ct);

    public Task<long> GetUnreadNotificationCountAsync(Guid userId, CancellationToken ct = default) =>
        db.Notifications.LongCountAsync(n => n.UserId == userId && !n.IsRead, ct);

    public Task<Notification?> GetNotificationByIdAsync(Guid id, CancellationToken ct = default) =>
        db.Notifications.FirstOrDefaultAsync(n => n.Id == id, ct);

    public async Task MarkAllNotificationsReadAsync(Guid userId, CancellationToken ct = default)
    {
        var unread = await db.Notifications.Where(n => n.UserId == userId && !n.IsRead).ToListAsync(ct);
        foreach (var n in unread) n.MarkRead();
    }

    public void Update(User user) => db.Users.Update(user);
    public async Task AddRefreshTokenAsync(RefreshToken token, CancellationToken ct = default) =>
        await db.RefreshTokens.AddAsync(token, ct);

    public Task<RefreshToken?> GetRefreshTokenByHashAsync(string tokenHash, CancellationToken ct = default) =>
        db.RefreshTokens.FirstOrDefaultAsync(r => r.TokenHash == tokenHash, ct);

    public void UpdateRefreshToken(RefreshToken token) => db.RefreshTokens.Update(token);
}

public class VitalRepository(AppDbContext db) : IVitalRepository
{
    public Task<VitalRecord?> GetByIdAsync(Guid id, CancellationToken ct = default) =>
        db.VitalRecords.FirstOrDefaultAsync(v => v.Id == id, ct);

    public async Task<IEnumerable<VitalRecord>> GetByPatientIdAsync(Guid patientId, DateTime from, DateTime to, int page, int pageSize, CancellationToken ct = default) =>
        await db.VitalRecords
            .Where(v => v.PatientId == patientId && v.RecordedAt >= from && v.RecordedAt <= to)
            .OrderByDescending(v => v.RecordedAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync(ct);

    public Task<VitalRecord?> GetLatestByPatientIdAsync(Guid patientId, CancellationToken ct = default) =>
        db.VitalRecords.Where(v => v.PatientId == patientId).OrderByDescending(v => v.RecordedAt).FirstOrDefaultAsync(ct);

    public Task<long> CountByPatientIdAsync(Guid patientId, DateTime from, DateTime to, CancellationToken ct = default) =>
        db.VitalRecords.LongCountAsync(v => v.PatientId == patientId && v.RecordedAt >= from && v.RecordedAt <= to, ct);

    public async Task AddAsync(VitalRecord record, CancellationToken ct = default) =>
        await db.VitalRecords.AddAsync(record, ct);
}

public class AlertRepository(AppDbContext db) : IAlertRepository
{
    public Task<Alert?> GetByIdAsync(Guid id, CancellationToken ct = default) =>
        db.Alerts.FirstOrDefaultAsync(a => a.Id == id, ct);

    public async Task<IEnumerable<Alert>> GetByPatientIdAsync(Guid patientId, int page, int pageSize, CancellationToken ct = default) =>
        await db.Alerts.Where(a => a.PatientId == patientId)
            .OrderByDescending(a => a.TriggeredAt)
            .Skip((page - 1) * pageSize).Take(pageSize).ToListAsync(ct);

    public async Task<IEnumerable<Alert>> GetUnresolvedByPatientIdAsync(Guid patientId, CancellationToken ct = default) =>
        await db.Alerts.Where(a => a.PatientId == patientId &&
            (a.Status == Domain.Enums.AlertStatus.Unread || a.Status == Domain.Enums.AlertStatus.Read))
            .OrderByDescending(a => a.TriggeredAt).ToListAsync(ct);

    // Threshold stored with PatientProfile.Id as FK.
    public Task<AlertThreshold?> GetThresholdByPatientIdAsync(Guid patientProfileId, CancellationToken ct = default) =>
        db.AlertThresholds.FirstOrDefaultAsync(t => t.PatientId == patientProfileId, ct);

    // Resolve User.Id → PatientProfile.Id via join, then fetch threshold.
    public Task<AlertThreshold?> GetThresholdByUserIdAsync(Guid userId, CancellationToken ct = default) =>
        db.AlertThresholds
            .Join(db.PatientProfiles,
                threshold => threshold.PatientId,
                profile   => profile.Id,
                (threshold, profile) => new { threshold, profile })
            .Where(x => x.profile.UserId == userId)
            .Select(x => x.threshold)
            .FirstOrDefaultAsync(ct);

    // Returns the most recent alert of a given type within the lookback window (for deduplication).
    public Task<Alert?> GetRecentAlertAsync(Guid patientId, Domain.Enums.AlertType type, TimeSpan lookback, CancellationToken ct = default)
    {
        var since = DateTime.UtcNow - lookback;
        return db.Alerts
            .Where(a => a.PatientId == patientId && a.Type == type && a.TriggeredAt >= since)
            .OrderByDescending(a => a.TriggeredAt)
            .FirstOrDefaultAsync(ct);
    }

    public async Task AddAsync(Alert alert, CancellationToken ct = default) =>
        await db.Alerts.AddAsync(alert, ct);

    public async Task AddThresholdAsync(AlertThreshold threshold, CancellationToken ct = default) =>
        await db.AlertThresholds.AddAsync(threshold, ct);

    public void Update(Alert alert) => db.Alerts.Update(alert);
    public void UpdateThreshold(AlertThreshold threshold) => db.AlertThresholds.Update(threshold);
}

public class ChatRepository(AppDbContext db) : IChatRepository
{
    public Task<Conversation?> GetByIdAsync(Guid id, CancellationToken ct = default) =>
        db.Conversations.Include(c => c.Participants).ThenInclude(p => p.User)
            .FirstOrDefaultAsync(c => c.Id == id, ct);

    public async Task<IEnumerable<Conversation>> GetByUserIdAsync(Guid userId, CancellationToken ct = default) =>
        await db.Conversations
            .Include(c => c.Participants).ThenInclude(p => p.User)
            .Where(c => c.Participants.Any(p => p.UserId == userId))
            .OrderByDescending(c => c.LastMessageAt).ToListAsync(ct);

    public Task<Conversation?> GetDirectConversationAsync(Guid userId1, Guid userId2, CancellationToken ct = default) =>
        db.Conversations.Include(c => c.Participants)
            .Where(c => c.Type == Domain.Enums.ConversationType.DoctorPatient || c.Type == Domain.Enums.ConversationType.DoctorRelative)
            .FirstOrDefaultAsync(c =>
                c.Participants.Any(p => p.UserId == userId1) &&
                c.Participants.Any(p => p.UserId == userId2), ct);

    public async Task<IEnumerable<Message>> GetMessagesByConversationIdAsync(Guid conversationId, int page, int pageSize, CancellationToken ct = default) =>
        await db.Messages.Include(m => m.Sender)
            .Where(m => m.ConversationId == conversationId && !m.IsDeleted)
            .OrderByDescending(m => m.SentAt)
            .Skip((page - 1) * pageSize).Take(pageSize)
            .OrderBy(m => m.SentAt).ToListAsync(ct);

    public Task<Message?> GetMessageByIdAsync(Guid messageId, CancellationToken ct = default) =>
        db.Messages.FirstOrDefaultAsync(m => m.Id == messageId, ct);

    public async Task AddConversationAsync(Conversation conversation, CancellationToken ct = default) =>
        await db.Conversations.AddAsync(conversation, ct);

    public async Task AddMessageAsync(Message message, CancellationToken ct = default) =>
        await db.Messages.AddAsync(message, ct);

    public async Task AddParticipantAsync(ConversationParticipant participant, CancellationToken ct = default) =>
        await db.ConversationParticipants.AddAsync(participant, ct);

    public void UpdateConversation(Conversation conversation) => db.Conversations.Update(conversation);
}

public class DeviceRepository(AppDbContext db) : IDeviceRepository
{
    public Task<Device?> GetByIdAsync(Guid id, CancellationToken ct = default) =>
        db.Devices.FirstOrDefaultAsync(d => d.Id == id, ct);

    public Task<Device?> GetByMqttClientIdAsync(string mqttClientId, CancellationToken ct = default) =>
        db.Devices.FirstOrDefaultAsync(d => d.MqttClientId == mqttClientId, ct);

    public async Task<IEnumerable<Device>> GetByPatientIdAsync(Guid patientId, CancellationToken ct = default) =>
        await db.Devices.Where(d => d.PatientId == patientId).ToListAsync(ct);

    public async Task AddAsync(Device device, CancellationToken ct = default) =>
        await db.Devices.AddAsync(device, ct);

    public void Update(Device device) => db.Devices.Update(device);
}

public class PatientRepository(AppDbContext db) : IPatientRepository
{
    public Task<PatientProfile?> GetByIdAsync(Guid id, CancellationToken ct = default) =>
        db.PatientProfiles.Include(p => p.DoctorAssignments).Include(p => p.RelativeLinks)
            .FirstOrDefaultAsync(p => p.Id == id, ct);

    public Task<PatientProfile?> GetByUserIdAsync(Guid userId, CancellationToken ct = default) =>
        db.PatientProfiles.Include(p => p.DoctorAssignments).Include(p => p.RelativeLinks)
            .FirstOrDefaultAsync(p => p.UserId == userId, ct);

    public Task<PatientProfile?> GetByShortPatientCodeAsync(string shortCode, CancellationToken ct = default) =>
        db.PatientProfiles.FirstOrDefaultAsync(
            p => p.ShortPatientCode == shortCode.ToUpperInvariant(), ct);

    public Task<bool> ShortPatientCodeExistsAsync(string shortCode, CancellationToken ct = default) =>
        db.PatientProfiles.AnyAsync(p => p.ShortPatientCode == shortCode.ToUpperInvariant(), ct);

    public Task<PatientProfile?> GetByPatientUserIdAsync(Guid userId, CancellationToken ct = default) =>
        db.PatientProfiles.Include(p => p.DoctorAssignments).ThenInclude(a => a.Doctor)
            .Include(p => p.RelativeLinks)
            .Include(p => p.User)
            .FirstOrDefaultAsync(p => p.UserId == userId, ct);

    public async Task<IEnumerable<PatientProfile>> GetByDoctorIdAsync(Guid doctorId, CancellationToken ct = default) =>
        await db.PatientProfiles.Include(p => p.User)
            .Where(p => p.DoctorAssignments.Any(a => a.DoctorId == doctorId && a.Status == Domain.Enums.RelationshipAssignmentStatus.Active))
            .ToListAsync(ct);

    public async Task<IEnumerable<PatientProfile>> GetByRelativeUserIdAsync(Guid relativeUserId, CancellationToken ct = default) =>
        await db.PatientProfiles.Include(p => p.User)
            .Where(p => p.RelativeLinks.Any(l => l.RelativeUserId == relativeUserId))
            .ToListAsync(ct);

    public Task<DoctorProfile?> GetDoctorProfileByUserIdAsync(Guid userId, CancellationToken ct = default) =>
        db.DoctorProfiles.FirstOrDefaultAsync(d => d.UserId == userId, ct);

    public async Task AddPatientProfileAsync(PatientProfile profile, CancellationToken ct = default) =>
        await db.PatientProfiles.AddAsync(profile, ct);

    public async Task AddDoctorProfileAsync(DoctorProfile profile, CancellationToken ct = default) =>
        await db.DoctorProfiles.AddAsync(profile, ct);

    public async Task AddAssignmentAsync(DoctorPatientAssignment assignment, CancellationToken ct = default) =>
        await db.DoctorPatientAssignments.AddAsync(assignment, ct);

    public async Task AddRelativeLinkAsync(PatientRelativeLink link, CancellationToken ct = default) =>
        await db.PatientRelativeLinks.AddAsync(link, ct);

    public void Update(PatientProfile profile) => db.PatientProfiles.Update(profile);
}
