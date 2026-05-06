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

    public Task<bool> ExistsByEmailAsync(string email, CancellationToken ct = default) =>
        db.Users.AnyAsync(u => u.Email == email.ToLowerInvariant(), ct);

    public async Task AddAsync(User user, CancellationToken ct = default) =>
        await db.Users.AddAsync(user, ct);

    public async Task AddNotificationAsync(Notification notification, CancellationToken ct = default) =>
        await db.Notifications.AddAsync(notification, ct);

    public void Update(User user) => db.Users.Update(user);
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

    public Task<AlertThreshold?> GetThresholdByPatientIdAsync(Guid patientId, CancellationToken ct = default) =>
        db.AlertThresholds.FirstOrDefaultAsync(t => t.PatientId == patientId, ct);

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

    public Task<PatientProfile?> GetByPatientUserIdAsync(Guid userId, CancellationToken ct = default) =>
        db.PatientProfiles.Include(p => p.DoctorAssignments).ThenInclude(a => a.Doctor)
            .Include(p => p.RelativeLinks)
            .Include(p => p.User)
            .FirstOrDefaultAsync(p => p.UserId == userId, ct);

    public async Task<IEnumerable<PatientProfile>> GetByDoctorIdAsync(Guid doctorId, CancellationToken ct = default) =>
        await db.PatientProfiles.Include(p => p.User)
            .Where(p => p.DoctorAssignments.Any(a => a.DoctorId == doctorId && a.Status == Domain.Enums.RelationshipAssignmentStatus.Active))
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
