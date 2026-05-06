using RPM.Domain.Common;
using RPM.Domain.Enums;
namespace RPM.Domain.Events;
public record AlertTriggeredEvent(Guid AlertId, Guid PatientId, AlertType Type, AlertSeverity Severity) : IDomainEvent;
