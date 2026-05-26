using MediatR;
using RPM.Application.Common.Exceptions;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Vitals;
using RPM.Application.Features.Vitals.Commands;
using RPM.Domain.Entities;
using RPM.Domain.Enums;
using RPM.Domain.Interfaces;
namespace RPM.Application.Features.Vitals.Handlers;

public class IngestVitalCommandHandler(IUnitOfWork uow, IVitalsHubService hub, ICacheService cache)
    : IRequestHandler<IngestVitalCommand, VitalRecordDto>
{
    public async Task<VitalRecordDto> Handle(IngestVitalCommand cmd, CancellationToken ct)
    {
        // Ensure device exists to satisfy FK constraint. If missing, create a patient profile (if needed)
        // and then create a placeholder device linked to the patient's profile.
        var existingDevice = await uow.Devices.GetByIdAsync(cmd.DeviceId, ct);
        if (existingDevice is null)
        {
            // Map incoming user-id (PatientId here is a User Id) to PatientProfile Id
            var profile = await uow.Patients.GetByUserIdAsync(cmd.PatientId, ct);
            if (profile is null)
            {
                // Ensure a User exists for this incoming patient id. If not, create a placeholder User.
                var existingUser = await uow.Users.GetByIdAsync(cmd.PatientId, ct);
                if (existingUser is null)
                {
                    var shortLabel = cmd.PatientId.ToString();
                    var placeholderUser = Domain.Entities.User.Create($"Patient {shortLabel}", $"patient+{shortLabel}@local", string.Empty, string.Empty, Domain.Enums.UserRole.Patient);
                    var idPropUser = typeof(Domain.Entities.User).GetProperty("Id");
                    if (idPropUser != null)
                    {
                        idPropUser.SetValue(placeholderUser, cmd.PatientId);
                    }
                    await uow.Users.AddAsync(placeholderUser, ct);
                    await uow.SaveChangesAsync(ct);
                }

                profile = Domain.Entities.PatientProfile.Create(cmd.PatientId);
                await uow.Patients.AddPatientProfileAsync(profile, ct);
                await uow.SaveChangesAsync(ct);
            }

            var mqttClientId = $"rpm-watch-{cmd.DeviceId:D}";
            var placeholder = Domain.Entities.Device.Create(profile.Id, "RPM Watch", "Samsung Watch", mqttClientId);
            // set Device Id to incoming device GUID so vitals reference match
            var idProp = typeof(Domain.Entities.Device).GetProperty("Id");
            if (idProp != null)
            {
                idProp.SetValue(placeholder, cmd.DeviceId);
            }

            await uow.Devices.AddAsync(placeholder, ct);
            await uow.SaveChangesAsync(ct);
        }

        var record = VitalRecord.Create(cmd.PatientId, cmd.DeviceId,
            cmd.HeartRateBpm, cmd.SpO2Percent, cmd.SystolicBp, cmd.DiastolicBp,
            cmd.TemperatureC, cmd.Steps, cmd.Calories, cmd.FallDetected, cmd.IsWearing);

        await uow.Vitals.AddAsync(record, ct);
        await uow.SaveChangesAsync(ct);

        var dto = MapToDto(record);
        await cache.SetAsync(LatestVitalsKey(cmd.PatientId), dto, TimeSpan.FromHours(6), ct);
        // Broadcast real-time
        await hub.BroadcastVitalsAsync(cmd.PatientId, dto, ct);

        return dto;
    }

    private static string LatestVitalsKey(Guid patientId) => $"patient:{patientId:D}:latest_vitals";

    private static VitalRecordDto MapToDto(VitalRecord r) =>
        new(r.Id, r.PatientId, r.DeviceId, r.HeartRateBpm, r.SpO2Percent,
            r.SystolicBp, r.DiastolicBp, r.TemperatureC, r.StepsCount,
            r.FallDetected, r.IsWearing, r.RecordedAt);
}

public class IngestVitalsBatchCommandHandler(IUnitOfWork uow, IVitalsHubService hub, ICacheService cache)
    : IRequestHandler<IngestVitalsBatchCommand, int>
{
    public async Task<int> Handle(IngestVitalsBatchCommand cmd, CancellationToken ct)
    {
        if (cmd.Readings.Count == 0) return 0;

        var createdUsers = new HashSet<Guid>();
        var createdProfiles = new Dictionary<Guid, PatientProfile>();
        var createdDevices = new HashSet<Guid>();
        var records = new List<VitalRecord>(cmd.Readings.Count);

        foreach (var reading in cmd.Readings)
        {
            var existingDevice = await uow.Devices.GetByIdAsync(reading.DeviceId, ct);
            if (existingDevice is null && createdDevices.Add(reading.DeviceId))
            {
                var profile = await EnsurePatientProfileAsync(reading.PatientId, createdUsers, createdProfiles, ct);
                var mqttClientId = $"rpm-watch-{reading.DeviceId:D}";
                var placeholder = Domain.Entities.Device.Create(profile.Id, "RPM Watch", "Samsung Watch", mqttClientId);
                var idProp = typeof(Domain.Entities.Device).GetProperty("Id");
                if (idProp != null)
                {
                    idProp.SetValue(placeholder, reading.DeviceId);
                }

                await uow.Devices.AddAsync(placeholder, ct);
            }

            records.Add(VitalRecord.Create(reading.PatientId, reading.DeviceId,
                reading.HeartRateBpm, reading.SpO2Percent, reading.SystolicBp, reading.DiastolicBp,
                reading.TemperatureC, reading.Steps, reading.Calories, reading.FallDetected, reading.IsWearing));
        }

        await uow.Vitals.AddRangeAsync(records, ct);
        await uow.SaveChangesAsync(ct);

        foreach (var record in records)
        {
            var dto = MapToDto(record);
            await cache.SetAsync(LatestVitalsKey(record.PatientId), dto, TimeSpan.FromHours(6), ct);
            await hub.BroadcastVitalsAsync(record.PatientId, dto, ct);
        }

        return records.Count;
    }

    private async Task<PatientProfile> EnsurePatientProfileAsync(
        Guid patientId,
        HashSet<Guid> createdUsers,
        Dictionary<Guid, PatientProfile> createdProfiles,
        CancellationToken ct)
    {
        if (createdProfiles.TryGetValue(patientId, out var cachedProfile))
            return cachedProfile;

        var profile = await uow.Patients.GetByUserIdAsync(patientId, ct);
        if (profile is not null) return profile;

        var existingUser = await uow.Users.GetByIdAsync(patientId, ct);
        if (existingUser is null && createdUsers.Add(patientId))
        {
            var shortLabel = patientId.ToString();
            var placeholderUser = Domain.Entities.User.Create($"Patient {shortLabel}", $"patient+{shortLabel}@local", string.Empty, string.Empty, Domain.Enums.UserRole.Patient);
            var idPropUser = typeof(Domain.Entities.User).GetProperty("Id");
            if (idPropUser != null)
            {
                idPropUser.SetValue(placeholderUser, patientId);
            }

            await uow.Users.AddAsync(placeholderUser, ct);
        }

        profile = Domain.Entities.PatientProfile.Create(patientId);
        createdProfiles[patientId] = profile;
        await uow.Patients.AddPatientProfileAsync(profile, ct);
        return profile;
    }

    private static string LatestVitalsKey(Guid patientId) => $"patient:{patientId:D}:latest_vitals";

    private static VitalRecordDto MapToDto(VitalRecord r) =>
        new(r.Id, r.PatientId, r.DeviceId, r.HeartRateBpm, r.SpO2Percent,
            r.SystolicBp, r.DiastolicBp, r.TemperatureC, r.StepsCount,
            r.FallDetected, r.IsWearing, r.RecordedAt);
}

public class UpdateAlertThresholdHandler(IUnitOfWork uow)
    : IRequestHandler<UpdateAlertThresholdCommand>
{
    public async Task Handle(UpdateAlertThresholdCommand cmd, CancellationToken ct)
    {
        var threshold = await uow.Alerts.GetThresholdByPatientIdAsync(cmd.PatientId, ct);
        if (threshold is null)
        {
            var t = AlertThreshold.CreateDefault(cmd.PatientId);
            t.Update(cmd.MinHeartRate, cmd.MaxHeartRate, cmd.MinSpO2, cmd.MaxSystolicBp, cmd.MaxDiastolicBp, cmd.MaxTemperatureC);
            await uow.Alerts.AddThresholdAsync(t, ct);
        }
        else
        {
            threshold.Update(cmd.MinHeartRate, cmd.MaxHeartRate, cmd.MinSpO2, cmd.MaxSystolicBp, cmd.MaxDiastolicBp, cmd.MaxTemperatureC);
            uow.Alerts.UpdateThreshold(threshold);
        }
        await uow.SaveChangesAsync(ct);
    }
}
