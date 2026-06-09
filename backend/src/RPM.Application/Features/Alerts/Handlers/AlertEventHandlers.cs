using MediatR;
using RPM.Application.Common.Interfaces;
using RPM.Domain.Entities;
using RPM.Domain.Enums;
using RPM.Domain.Events;
using RPM.Domain.Interfaces;
namespace RPM.Application.Features.Alerts.Handlers;

public class AlertTriggeredEventHandler(IUnitOfWork uow, INotificationService notif, IVitalsHubService hub)
    : INotificationHandler<AlertTriggeredEvent>
{
    public async Task Handle(AlertTriggeredEvent evt, CancellationToken ct)
    {
        var alert = await uow.Alerts.GetByIdAsync(evt.AlertId, ct);
        if (alert is null) return;

        var patient = await uow.Patients.GetByPatientUserIdAsync(evt.PatientId, ct);
        var patientUser = await uow.Users.GetByIdAsync(evt.PatientId, ct);
        var tokens = new List<string>();
        var recipientUserIds = new HashSet<Guid>();

        if (patient is not null)
        {
            foreach (var assignment in patient.DoctorAssignments.Where(a => a.Status == RelationshipAssignmentStatus.Active))
            {
                var doctor = await uow.Users.GetByIdAsync(assignment.DoctorId, ct);
                if (doctor is null) continue;
                recipientUserIds.Add(doctor.Id);
                if (!string.IsNullOrWhiteSpace(doctor.FcmToken)) tokens.Add(doctor.FcmToken!);
            }
            foreach (var link in patient.RelativeLinks)
            {
                var rel = await uow.Users.GetByIdAsync(link.RelativeUserId, ct);
                if (rel is null) continue;
                recipientUserIds.Add(rel.Id);
                if (!string.IsNullOrWhiteSpace(rel.FcmToken)) tokens.Add(rel.FcmToken!);
            }
        }

        if (!string.IsNullOrWhiteSpace(patientUser?.FcmToken))
            tokens.Add(patientUser.FcmToken!);

        var title = evt.Severity switch
        {
            AlertSeverity.Critical => $"CRITICAL: {patientUser?.FullName ?? "patient"}",
            AlertSeverity.High => $"Urgent alert: {patientUser?.FullName ?? "patient"}",
            _ => $"Alert: {patientUser?.FullName ?? "patient"}"
        };

        if (tokens.Count > 0)
        {
            await notif.SendPushToManyAsync(tokens, title, alert.Message,
                new Dictionary<string, string>
                {
                    ["title"] = title,
                    ["body"] = alert.Message,
                    ["alertId"] = evt.AlertId.ToString(),
                    ["patientId"] = evt.PatientId.ToString(),
                    ["type"] = "alert",
                    ["severity"] = evt.Severity.ToString()
                },
                channelId: "rpm_alerts", ct: ct);
        }

        await hub.BroadcastAlertAsync(evt.PatientId, new
        {
            alert.Id,
            alert.Message,
            Type = alert.Type.ToString(),
            Severity = alert.Severity.ToString(),
            alert.TriggeredAt,
            PatientId = evt.PatientId,
            PatientName = patientUser?.FullName
        }, ct);

        // Persist a notification row per recipient (doctors + relatives) so the dashboards
        // can render an inbox of past alerts even if the FCM push was missed.
        var dataPayload = $"{{\"alertId\":\"{evt.AlertId}\",\"patientId\":\"{evt.PatientId}\",\"type\":\"{evt.Type}\",\"severity\":\"{evt.Severity}\"}}";
        foreach (var recipientId in recipientUserIds)
        {
            var record = Notification.Create(recipientId, title, alert.Message, evt.AlertId, dataPayload);
            await uow.Users.AddNotificationAsync(record, ct);
        }

        // Always also keep one notification on the patient's own timeline for personal history.
        var patientRecord = Notification.Create(evt.PatientId, title, alert.Message, evt.AlertId, dataPayload);
        await uow.Users.AddNotificationAsync(patientRecord, ct);

        await uow.SaveChangesAsync(ct);
    }
}

public class VitalRecordedEventHandler(IUnitOfWork uow)
    : INotificationHandler<VitalRecordedEvent>
{
    public async Task Handle(VitalRecordedEvent evt, CancellationToken ct)
    {
        var record = await uow.Vitals.GetByIdAsync(evt.VitalRecordId, ct);
        if (record is null) return;

        var threshold = await uow.Alerts.GetThresholdByPatientIdAsync(evt.PatientId, ct);
        if (threshold is null) return;

        var alerts = new List<Alert>();

        if (record.HeartRateBpm.HasValue)
        {
            if (record.HeartRateBpm > threshold.MaxHeartRate)
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighHeartRate, AlertSeverity.High,
                    $"Heart rate {record.HeartRateBpm:F0} bpm exceeds maximum {threshold.MaxHeartRate:F0} bpm."));
            else if (record.HeartRateBpm < threshold.MinHeartRate)
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.LowHeartRate, AlertSeverity.High,
                    $"Heart rate {record.HeartRateBpm:F0} bpm below minimum {threshold.MinHeartRate:F0} bpm."));
        }

        if (record.SpO2Percent.HasValue && record.SpO2Percent < threshold.MinSpO2)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.LowSpO2, AlertSeverity.Critical,
                $"SpO2 {record.SpO2Percent:F1}% below minimum {threshold.MinSpO2:F0}%."));

        if (record.FallDetected)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.FallDetected, AlertSeverity.Critical,
                "Fall detected. Patient may need immediate assistance."));

        if (record.SystolicBp.HasValue && record.SystolicBp > threshold.MaxSystolicBp)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighBloodPressure, AlertSeverity.High,
                $"Systolic BP {record.SystolicBp:F0} mmHg exceeds maximum {threshold.MaxSystolicBp:F0} mmHg."));

        if (record.TemperatureC.HasValue && record.TemperatureC > threshold.MaxTemperatureC)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighTemperature, AlertSeverity.Medium,
                $"Body temperature {record.TemperatureC:F1}°C exceeds maximum {threshold.MaxTemperatureC:F1}°C."));

        if (record.SkinTemperatureC.HasValue && record.SkinTemperatureC > threshold.MaxSkinTemperatureC)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighSkinTemperature, AlertSeverity.Medium,
                $"Skin temperature {record.SkinTemperatureC:F1}°C above expected {threshold.MaxSkinTemperatureC:F1}°C."));

        if (record.RespirationRateBpm.HasValue)
        {
            if (record.RespirationRateBpm < threshold.MinRespirationRate)
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.AbnormalRespirationRate, AlertSeverity.Medium,
                    $"Respiration rate {record.RespirationRateBpm:F0}/min below minimum {threshold.MinRespirationRate:F0}/min."));
            else if (record.RespirationRateBpm > threshold.MaxRespirationRate)
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.AbnormalRespirationRate, AlertSeverity.Medium,
                    $"Respiration rate {record.RespirationRateBpm:F0}/min exceeds maximum {threshold.MaxRespirationRate:F0}/min."));
        }

        if (record.StressScore.HasValue && record.StressScore > threshold.MaxStressScore)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighStress, AlertSeverity.Low,
                $"Stress score {record.StressScore:F0} above threshold {threshold.MaxStressScore:F0}."));

        if (record.BloodGlucoseMgDl.HasValue)
        {
            if (record.BloodGlucoseMgDl < threshold.MinBloodGlucoseMgDl)
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.LowBloodGlucose, AlertSeverity.High,
                    $"Blood glucose {record.BloodGlucoseMgDl:F0} mg/dL below minimum {threshold.MinBloodGlucoseMgDl:F0} mg/dL."));
            else if (record.BloodGlucoseMgDl > threshold.MaxBloodGlucoseMgDl)
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighBloodGlucose, AlertSeverity.High,
                    $"Blood glucose {record.BloodGlucoseMgDl:F0} mg/dL exceeds maximum {threshold.MaxBloodGlucoseMgDl:F0} mg/dL."));
        }

        if (!string.IsNullOrWhiteSpace(record.EcgClassification) &&
            !record.EcgClassification.Equals("Sinus Rhythm", StringComparison.OrdinalIgnoreCase) &&
            !record.EcgClassification.Equals("Normal", StringComparison.OrdinalIgnoreCase))
        {
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.AbnormalEcg, AlertSeverity.High,
                $"ECG reported {record.EcgClassification}."));
        }

        if (record.BatteryLevel.HasValue && record.BatteryLevel < 15)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.LowBattery, AlertSeverity.Low,
                $"Watch battery is low: {record.BatteryLevel:F0}%."));

        foreach (var alert in alerts)
            await uow.Alerts.AddAsync(alert, ct);

        if (alerts.Count > 0)
            await uow.SaveChangesAsync(ct);
    }
}
