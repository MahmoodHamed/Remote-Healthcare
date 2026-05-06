using RPM.Domain.Common;
namespace RPM.Domain.Events;
public record VitalRecordedEvent(Guid VitalRecordId, Guid PatientId, Guid DeviceId) : IDomainEvent;
