using MediatR;
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
        var existingDevice = await uow.Devices.GetByIdAsync(cmd.DeviceId, ct);
        if (existingDevice is null)
        {
            var profile = await EnsurePatientProfileAsync(uow, cmd.PatientId, ct);
            await EnsureDeviceAsync(uow, cmd.DeviceId, profile, ct);
            await uow.SaveChangesAsync(ct);
            existingDevice = await uow.Devices.GetByIdAsync(cmd.DeviceId, ct);
        }

        existingDevice?.UpdateStatus(DeviceStatus.Online, cmd.BatteryLevel);
        if (existingDevice is not null) uow.Devices.Update(existingDevice);

        var record = VitalRecord.Create(
            cmd.PatientId,
            cmd.DeviceId,
            cmd.HeartRateBpm,
            cmd.SpO2Percent,
            cmd.SystolicBp,
            cmd.DiastolicBp,
            cmd.TemperatureC,
            cmd.Steps,
            cmd.Calories,
            cmd.FallDetected,
            cmd.IsWearing,
            cmd.SkinTemperatureC,
            cmd.HeartRateVariabilityMs,
            cmd.RestingHeartRateBpm,
            cmd.MaxHeartRateBpm,
            cmd.RespirationRateBpm,
            cmd.DistanceMeters,
            cmd.FloorsClimbed,
            cmd.ActiveMinutes,
            cmd.StressScore,
            cmd.SleepScore,
            cmd.SleepDurationMinutes,
            cmd.BodyFatPercent,
            cmd.MuscleMassKg,
            cmd.BodyWaterPercent,
            cmd.BasalMetabolicRate,
            cmd.EcgAverageHeartRate,
            cmd.EcgClassification,
            cmd.EcgWaveformJson,
            cmd.BloodGlucoseMgDl,
            cmd.BatteryLevel);

        await uow.Vitals.AddAsync(record, ct);
        await uow.SaveChangesAsync(ct);

        var dto = VitalMapper.ToDto(record);
        await cache.SetAsync(VitalMapper.LatestVitalsKey(cmd.PatientId), dto, TimeSpan.FromHours(6), ct);
        await hub.BroadcastVitalsAsync(cmd.PatientId, dto, ct);

        return dto;
    }

    internal static async Task<PatientProfile> EnsurePatientProfileAsync(IUnitOfWork uow, Guid userId, CancellationToken ct)
    {
        var profile = await uow.Patients.GetByUserIdAsync(userId, ct);
        if (profile is not null) return profile;

        var existingUser = await uow.Users.GetByIdAsync(userId, ct);
        if (existingUser is null)
        {
            var shortLabel = userId.ToString("N")[..8];
            var placeholderUser = User.Create(
                $"Patient {shortLabel}",
                $"patient+{shortLabel}@local",
                string.Empty,
                string.Empty,
                Domain.Enums.UserRole.Patient);
            typeof(User).GetProperty("Id")?.SetValue(placeholderUser, userId);
            await uow.Users.AddAsync(placeholderUser, ct);
            await uow.SaveChangesAsync(ct);
        }

        profile = PatientProfile.Create(userId);
        await uow.Patients.AddPatientProfileAsync(profile, ct);
        await uow.SaveChangesAsync(ct);
        return profile;
    }

    internal static async Task EnsureDeviceAsync(IUnitOfWork uow, Guid deviceId, PatientProfile profile, CancellationToken ct)
    {
        var mqttClientId = $"rpm-watch-{deviceId:D}";
        var device = Device.Create(profile.Id, "RPM Watch", "Samsung Galaxy Watch 8", mqttClientId);
        typeof(Device).GetProperty("Id")?.SetValue(device, deviceId);
        await uow.Devices.AddAsync(device, ct);
    }
}

public class IngestVitalsBatchCommandHandler(IUnitOfWork uow, IVitalsHubService hub, ICacheService cache)
    : IRequestHandler<IngestVitalsBatchCommand, int>
{
    public async Task<int> Handle(IngestVitalsBatchCommand cmd, CancellationToken ct)
    {
        if (cmd.Readings.Count == 0) return 0;

        var profilesByUser = new Dictionary<Guid, PatientProfile>();
        var ensuredDevices = new HashSet<Guid>();
        var records = new List<VitalRecord>(cmd.Readings.Count);

        foreach (var reading in cmd.Readings)
        {
            if (!profilesByUser.TryGetValue(reading.PatientId, out var profile))
            {
                profile = await IngestVitalCommandHandler.EnsurePatientProfileAsync(uow, reading.PatientId, ct);
                profilesByUser[reading.PatientId] = profile;
            }

            var device = await uow.Devices.GetByIdAsync(reading.DeviceId, ct);
            if (device is null && ensuredDevices.Add(reading.DeviceId))
            {
                await IngestVitalCommandHandler.EnsureDeviceAsync(uow, reading.DeviceId, profile, ct);
                device = await uow.Devices.GetByIdAsync(reading.DeviceId, ct);
            }

            device?.UpdateStatus(DeviceStatus.Online, reading.BatteryLevel);
            if (device is not null) uow.Devices.Update(device);

            records.Add(VitalRecord.Create(
                reading.PatientId,
                reading.DeviceId,
                reading.HeartRateBpm,
                reading.SpO2Percent,
                reading.SystolicBp,
                reading.DiastolicBp,
                reading.TemperatureC,
                reading.Steps,
                reading.Calories,
                reading.FallDetected,
                reading.IsWearing,
                reading.SkinTemperatureC,
                reading.HeartRateVariabilityMs,
                reading.RestingHeartRateBpm,
                reading.MaxHeartRateBpm,
                reading.RespirationRateBpm,
                reading.DistanceMeters,
                reading.FloorsClimbed,
                reading.ActiveMinutes,
                reading.StressScore,
                reading.SleepScore,
                reading.SleepDurationMinutes,
                reading.BodyFatPercent,
                reading.MuscleMassKg,
                reading.BodyWaterPercent,
                reading.BasalMetabolicRate,
                reading.EcgAverageHeartRate,
                reading.EcgClassification,
                reading.EcgWaveformJson,
                reading.BloodGlucoseMgDl,
                reading.BatteryLevel));
        }

        await uow.Vitals.AddRangeAsync(records, ct);
        await uow.SaveChangesAsync(ct);

        foreach (var record in records)
        {
            var dto = VitalMapper.ToDto(record);
            await cache.SetAsync(VitalMapper.LatestVitalsKey(record.PatientId), dto, TimeSpan.FromHours(6), ct);
            await hub.BroadcastVitalsAsync(record.PatientId, dto, ct);
        }

        return records.Count;
    }
}

public class UpdateAlertThresholdHandler(IUnitOfWork uow)
    : IRequestHandler<UpdateAlertThresholdCommand>
{
    public async Task Handle(UpdateAlertThresholdCommand cmd, CancellationToken ct)
    {
        var threshold = await uow.Alerts.GetThresholdByPatientIdAsync(cmd.PatientId, ct);
        if (threshold is null)
        {
            threshold = AlertThreshold.CreateDefault(cmd.PatientId);
            threshold.Update(cmd.MinHeartRate, cmd.MaxHeartRate, cmd.MinSpO2, cmd.MaxSystolicBp, cmd.MaxDiastolicBp, cmd.MaxTemperatureC);
            threshold.UpdateAdvanced(cmd.MaxSkinTemperatureC, cmd.MinRespirationRate, cmd.MaxRespirationRate, cmd.MaxStressScore, cmd.MinBloodGlucoseMgDl, cmd.MaxBloodGlucoseMgDl);
            await uow.Alerts.AddThresholdAsync(threshold, ct);
        }
        else
        {
            threshold.Update(cmd.MinHeartRate, cmd.MaxHeartRate, cmd.MinSpO2, cmd.MaxSystolicBp, cmd.MaxDiastolicBp, cmd.MaxTemperatureC);
            threshold.UpdateAdvanced(cmd.MaxSkinTemperatureC, cmd.MinRespirationRate, cmd.MaxRespirationRate, cmd.MaxStressScore, cmd.MinBloodGlucoseMgDl, cmd.MaxBloodGlucoseMgDl);
            uow.Alerts.UpdateThreshold(threshold);
        }
        await uow.SaveChangesAsync(ct);
    }
}

internal static class VitalMapper
{
    public static string LatestVitalsKey(Guid patientId) => $"patient:{patientId:D}:latest_vitals";

    public static VitalRecordDto ToDto(VitalRecord r) => new(
        r.Id,
        r.PatientId,
        r.DeviceId,
        r.HeartRateBpm,
        r.SpO2Percent,
        r.SystolicBp,
        r.DiastolicBp,
        r.TemperatureC,
        r.StepsCount,
        r.CaloriesBurned,
        r.FallDetected,
        r.IsWearing,
        r.RecordedAt,
        r.SkinTemperatureC,
        r.HeartRateVariabilityMs,
        r.RestingHeartRateBpm,
        r.MaxHeartRateBpm,
        r.RespirationRateBpm,
        r.DistanceMeters,
        r.FloorsClimbed,
        r.ActiveMinutes,
        r.StressScore,
        r.SleepScore,
        r.SleepDurationMinutes,
        r.BodyFatPercent,
        r.MuscleMassKg,
        r.BodyWaterPercent,
        r.BasalMetabolicRate,
        r.EcgAverageHeartRate,
        r.EcgClassification,
        r.EcgWaveformJson,
        r.BloodGlucoseMgDl,
        r.BatteryLevel);
}
