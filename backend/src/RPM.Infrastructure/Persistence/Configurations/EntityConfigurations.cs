using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using RPM.Domain.Entities;
namespace RPM.Infrastructure.Persistence.Configurations;

public class UserConfiguration : IEntityTypeConfiguration<User>
{
    public void Configure(EntityTypeBuilder<User> b)
    {
        b.HasKey(x => x.Id);
        b.Property(x => x.Email).IsRequired().HasMaxLength(256);
        b.Property(x => x.FullName).IsRequired().HasMaxLength(100);
        b.Property(x => x.Phone).HasMaxLength(20);
        b.Property(x => x.PasswordHash).IsRequired();
        b.Property(x => x.Role).HasConversion<string>();
        b.HasIndex(x => x.Email).IsUnique();
        b.HasOne(x => x.DoctorProfile).WithOne(x => x.User).HasForeignKey<DoctorProfile>(x => x.UserId);
        b.HasOne(x => x.PatientProfile).WithOne(x => x.User).HasForeignKey<PatientProfile>(x => x.UserId);
    }
}

public class VitalRecordConfiguration : IEntityTypeConfiguration<VitalRecord>
{
    public void Configure(EntityTypeBuilder<VitalRecord> b)
    {
        b.HasKey(x => x.Id);
        b.Property(x => x.RecordedAt).IsRequired();
        b.HasIndex(x => new { x.PatientId, x.RecordedAt });
        // TimescaleDB: partition key is RecordedAt - run migration manually:
        // SELECT create_hypertable('"VitalRecords"', 'RecordedAt');
    }
}

public class AlertConfiguration : IEntityTypeConfiguration<Alert>
{
    public void Configure(EntityTypeBuilder<Alert> b)
    {
        b.HasKey(x => x.Id);
        b.Property(x => x.Type).HasConversion<string>();
        b.Property(x => x.Severity).HasConversion<string>();
        b.Property(x => x.Status).HasConversion<string>();
        b.HasIndex(x => new { x.PatientId, x.Status });
    }
}

public class MessageConfiguration : IEntityTypeConfiguration<Message>
{
    public void Configure(EntityTypeBuilder<Message> b)
    {
        b.HasKey(x => x.Id);
        b.Property(x => x.Type).HasConversion<string>();
        b.HasIndex(x => new { x.ConversationId, x.SentAt });
    }
}

public class ConversationConfiguration : IEntityTypeConfiguration<Conversation>
{
    public void Configure(EntityTypeBuilder<Conversation> b)
    {
        b.HasKey(x => x.Id);
        b.Property(x => x.Type).HasConversion<string>();
    }
}

public class PatientProfileConfiguration : IEntityTypeConfiguration<PatientProfile>
{
    public void Configure(EntityTypeBuilder<PatientProfile> b)
    {
        b.HasKey(x => x.Id);
        b.Property(x => x.BloodType).HasConversion<string>();
        b.Property(x => x.ChronicDiseases).HasColumnType("jsonb");
        b.Property(x => x.Allergies).HasColumnType("jsonb");
        b.Property(x => x.CurrentMedications).HasColumnType("jsonb");
    }
}

public class DeviceConfiguration : IEntityTypeConfiguration<Device>
{
    public void Configure(EntityTypeBuilder<Device> b)
    {
        b.HasKey(x => x.Id);
        b.Property(x => x.Status).HasConversion<string>();
        b.HasIndex(x => x.MqttClientId).IsUnique();
    }
}

public class RefreshTokenConfiguration : IEntityTypeConfiguration<RefreshToken>
{
    public void Configure(EntityTypeBuilder<RefreshToken> b)
    {
        b.HasKey(x => x.Id);
        b.HasIndex(x => x.TokenHash);
    }
}
