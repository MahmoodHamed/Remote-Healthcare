using MediatR;
using RPM.Application.Common.Exceptions;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Vitals;
using RPM.Application.Features.Vitals.Commands;
using RPM.Domain.Entities;
using RPM.Domain.Enums;
using RPM.Domain.Interfaces;
namespace RPM.Application.Features.Vitals.Handlers;

public class IngestVitalCommandHandler(IUnitOfWork uow, IVitalsHubService hub)
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

            var placeholder = Domain.Entities.Device.Create(profile.Id, "unknown", "unknown", string.Empty);
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
            cmd.TemperatureC, cmd.SkinTemperatureC, cmd.AmbientTemperatureC, cmd.HrvMs,
            cmd.StressScore, cmd.BodyFatPercent, cmd.EcgAvgHeartRateBpm,
            cmd.Steps, cmd.Calories, cmd.FallDetected, cmd.IsWearing);

        await uow.Vitals.AddAsync(record, ct);

        // Keep the device's last-seen timestamp and status current
        var device = await uow.Devices.GetByIdAsync(cmd.DeviceId, ct);
        if (device is not null)
        {
            var newStatus = Domain.Enums.DeviceStatus.Online;
            device.UpdateStatus(newStatus);
            uow.Devices.Update(device);
        }

        await uow.SaveChangesAsync(ct);

        var dto = VitalRecordDtoMapper.MapToDto(record);
        await hub.BroadcastVitalsAsync(cmd.PatientId, dto, ct);

        return dto;
    }
}

public class UpdateAlertThresholdHandler(IUnitOfWork uow)
    : IRequestHandler<UpdateAlertThresholdCommand>
{
    public async Task Handle(UpdateAlertThresholdCommand cmd, CancellationToken ct)
    {
        // cmd.PatientId is User.Id. AlertThreshold.PatientId is PatientProfile.Id, so we
        // resolve the profile first to get the correct FK value.
        var profile = await uow.Patients.GetByUserIdAsync(cmd.PatientId, ct)
            ?? throw new InvalidOperationException($"No patient profile found for user {cmd.PatientId}");

        var threshold = await uow.Alerts.GetThresholdByPatientIdAsync(profile.Id, ct);
        if (threshold is null)
        {
            var t = AlertThreshold.CreateDefault(profile.Id);
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
