using Microsoft.EntityFrameworkCore;
using RPM.Domain.Common;
using RPM.Domain.Entities;
using RPM.Domain.Interfaces;
namespace RPM.Infrastructure.Persistence;

public class AppDbContext(DbContextOptions<AppDbContext> options) : DbContext(options)
{
    public DbSet<User> Users => Set<User>();
    public DbSet<DoctorProfile> DoctorProfiles => Set<DoctorProfile>();
    public DbSet<PatientProfile> PatientProfiles => Set<PatientProfile>();
    public DbSet<DoctorPatientAssignment> DoctorPatientAssignments => Set<DoctorPatientAssignment>();
    public DbSet<PatientRelativeLink> PatientRelativeLinks => Set<PatientRelativeLink>();
    public DbSet<Device> Devices => Set<Device>();
    public DbSet<VitalRecord> VitalRecords => Set<VitalRecord>();
    public DbSet<AlertThreshold> AlertThresholds => Set<AlertThreshold>();
    public DbSet<Alert> Alerts => Set<Alert>();
    public DbSet<Conversation> Conversations => Set<Conversation>();
    public DbSet<ConversationParticipant> ConversationParticipants => Set<ConversationParticipant>();
    public DbSet<Message> Messages => Set<Message>();
    public DbSet<MessageRead> MessageReads => Set<MessageRead>();
    public DbSet<Notification> Notifications => Set<Notification>();
    public DbSet<RefreshToken> RefreshTokens => Set<RefreshToken>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(AppDbContext).Assembly);
        // TimescaleDB hypertable for VitalRecords
        modelBuilder.HasPostgresExtension("timescaledb");
    }

    public override async Task<int> SaveChangesAsync(CancellationToken ct = default)
    {
        // Dispatch domain events before saving
        var entities = ChangeTracker.Entries<BaseEntity>()
            .Where(e => e.Entity.DomainEvents.Any())
            .Select(e => e.Entity).ToList();

        var result = await base.SaveChangesAsync(ct);

        // Dispatch after save to ensure IDs are assigned
        foreach (var entity in entities)
        {
            var events = entity.DomainEvents.ToList();
            entity.ClearDomainEvents();
            foreach (var evt in events)
                await _mediator.Publish(evt, ct);
        }

        return result;
    }

    private MediatR.IMediator _mediator = null!;
    public void SetMediator(MediatR.IMediator mediator) => _mediator = mediator;
}
