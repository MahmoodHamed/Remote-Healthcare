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

        // Get doctor and relatives to notify
        var patient = await uow.Patients.GetByPatientUserIdAsync(evt.PatientId, ct);
        var tokens = new List<string>();

        if (patient is not null)
        {
            foreach (var assignment in patient.DoctorAssignments.Where(a => a.Status == RelationshipAssignmentStatus.Active))
            {
                var doc = await uow.Users.GetByIdAsync(assignment.DoctorId, ct);
                if (doc?.FcmToken != null) tokens.Add(doc.FcmToken);
            }
            foreach (var link in patient.RelativeLinks)
            {
                var rel = await uow.Users.GetByIdAsync(link.RelativeUserId, ct);
                if (rel?.FcmToken != null) tokens.Add(rel.FcmToken);
            }
        }

        if (tokens.Count > 0)
        {
            var title = evt.Severity == AlertSeverity.Critical ? "🚨 CRITICAL Alert" : "⚠️ Patient Alert";
            var body = alert.Message;
            await notif.SendPushToManyAsync(tokens, title, body,
                new Dictionary<string, string>
                {
                    ["alertId"] = evt.AlertId.ToString(),
                    ["patientId"] = evt.PatientId.ToString(),
                    ["type"] = evt.Type.ToString(),
                    ["severity"] = evt.Severity.ToString()
                }, ct);
        }

        // Real-time hub broadcast
        await hub.BroadcastAlertAsync(evt.PatientId, new { alert.Id, alert.Message, alert.Type, alert.Severity }, ct);

        // Persist notification record
        var notifRecord = Notification.Create(evt.PatientId, "Patient Alert", alert.Message, evt.AlertId);
        await uow.Users.AddNotificationAsync(notifRecord, ct);
        await uow.SaveChangesAsync(ct);
    }
}

public class VitalRecordedEventHandler(IUnitOfWork uow)
    : INotificationHandler<VitalRecordedEvent>
{
    // Minimum time between duplicate alerts of the same type for the same patient.
    private static readonly TimeSpan FallAlertCooldown    = TimeSpan.FromMinutes(5);
    private static readonly TimeSpan VitalAlertCooldown   = TimeSpan.FromMinutes(2);

    // Sanity ranges — values outside these are sensor artifacts, not patient conditions.
    private static bool IsValidHr(float v)   => v is >= 30 and <= 220;
    private static bool IsValidSpo2(float v) => v is >= 70 and <= 100;
    private static bool IsValidTemp(float v) => v is >= 34 and <= 43;
    private static bool IsValidBp(float v)   => v is >= 50 and <= 250;

    public async Task Handle(VitalRecordedEvent evt, CancellationToken ct)
    {
        var record = await uow.Vitals.GetByIdAsync(evt.VitalRecordId, ct);
        if (record is null) return;

        // evt.PatientId is User.Id; AlertThreshold.PatientId is PatientProfile.Id — use the join method.
        var threshold = await uow.Alerts.GetThresholdByUserIdAsync(evt.PatientId, ct);
        if (threshold is null) return;

        var alerts = new List<Alert>();

        if (record.HeartRateBpm.HasValue && IsValidHr(record.HeartRateBpm.Value))
        {
            if (record.HeartRateBpm > threshold.MaxHeartRate)
            {
                if (await NoDuplicateAlert(evt.PatientId, AlertType.HighHeartRate, VitalAlertCooldown, ct))
                    alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighHeartRate, AlertSeverity.High,
                        $"Heart rate is {record.HeartRateBpm:F0} bpm — above limit {threshold.MaxHeartRate} bpm"));
            }
            else if (record.HeartRateBpm < threshold.MinHeartRate)
            {
                if (await NoDuplicateAlert(evt.PatientId, AlertType.LowHeartRate, VitalAlertCooldown, ct))
                    alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.LowHeartRate, AlertSeverity.High,
                        $"Heart rate is {record.HeartRateBpm:F0} bpm — below limit {threshold.MinHeartRate} bpm"));
            }
        }

        if (record.SpO2Percent.HasValue && IsValidSpo2(record.SpO2Percent.Value)
            && record.SpO2Percent < threshold.MinSpO2)
        {
            if (await NoDuplicateAlert(evt.PatientId, AlertType.LowSpO2, VitalAlertCooldown, ct))
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.LowSpO2, AlertSeverity.Critical,
                    $"SpO₂ is {record.SpO2Percent:F1}% — below limit {threshold.MinSpO2}%"));
        }

        if (record.FallDetected)
        {
            // 5-minute cooldown: the watch latches fall=true for 15 s, generating many MQTT messages.
            if (await NoDuplicateAlert(evt.PatientId, AlertType.FallDetected, FallAlertCooldown, ct))
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.FallDetected, AlertSeverity.Critical,
                    "Fall detected — patient may need immediate assistance."));
        }

        if (record.SystolicBp.HasValue && IsValidBp(record.SystolicBp.Value)
            && record.SystolicBp > threshold.MaxSystolicBp)
        {
            if (await NoDuplicateAlert(evt.PatientId, AlertType.HighBloodPressure, VitalAlertCooldown, ct))
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighBloodPressure, AlertSeverity.High,
                    $"Systolic BP is {record.SystolicBp:F0} mmHg — above limit {threshold.MaxSystolicBp} mmHg"));
        }

        if (record.TemperatureC.HasValue && IsValidTemp(record.TemperatureC.Value)
            && record.TemperatureC > threshold.MaxTemperatureC)
        {
            if (await NoDuplicateAlert(evt.PatientId, AlertType.HighTemperature, VitalAlertCooldown, ct))
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighTemperature, AlertSeverity.Medium,
                    $"Temperature is {record.TemperatureC:F1}°C — above limit {threshold.MaxTemperatureC}°C"));
        }

        foreach (var alert in alerts)
            await uow.Alerts.AddAsync(alert, ct);

        if (alerts.Count > 0)
            await uow.SaveChangesAsync(ct);
    }

    private async Task<bool> NoDuplicateAlert(Guid patientId, AlertType type, TimeSpan cooldown, CancellationToken ct)
    {
        var recent = await uow.Alerts.GetRecentAlertAsync(patientId, type, cooldown, ct);
        return recent is null;
    }
}
