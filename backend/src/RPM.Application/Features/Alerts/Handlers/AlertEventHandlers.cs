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
                    $"Heart rate is {record.HeartRateBpm} bpm - above maximum {threshold.MaxHeartRate} bpm"));
            else if (record.HeartRateBpm < threshold.MinHeartRate)
                alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.LowHeartRate, AlertSeverity.High,
                    $"Heart rate is {record.HeartRateBpm} bpm - below minimum {threshold.MinHeartRate} bpm"));
        }
        if (record.SpO2Percent.HasValue && record.SpO2Percent < threshold.MinSpO2)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.LowSpO2, AlertSeverity.Critical,
                $"SpO2 is {record.SpO2Percent}% - below minimum {threshold.MinSpO2}%"));

        if (record.FallDetected)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.FallDetected, AlertSeverity.Critical,
                "Fall detected! Patient may need immediate assistance."));

        if (record.SystolicBp.HasValue && record.SystolicBp > threshold.MaxSystolicBp)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighBloodPressure, AlertSeverity.High,
                $"Systolic BP is {record.SystolicBp} mmHg - above maximum {threshold.MaxSystolicBp} mmHg"));

        if (record.TemperatureC.HasValue && record.TemperatureC > threshold.MaxTemperatureC)
            alerts.Add(Alert.Create(evt.PatientId, evt.VitalRecordId, AlertType.HighTemperature, AlertSeverity.Medium,
                $"Temperature is {record.TemperatureC}°C - above maximum {threshold.MaxTemperatureC}°C"));

        foreach (var alert in alerts)
            await uow.Alerts.AddAsync(alert, ct);

        if (alerts.Count > 0)
            await uow.SaveChangesAsync(ct);
    }
}
