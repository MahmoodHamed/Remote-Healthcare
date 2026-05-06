using MediatR;
using RPM.Application.DTOs.Vitals;
namespace RPM.Application.Features.Vitals.Queries;

public record GetPatientVitalsQuery(Guid PatientId, DateTime From, DateTime To, int Page = 1, int PageSize = 50) : IRequest<VitalsPagedDto>;
public record GetLatestVitalsQuery(Guid PatientId) : IRequest<VitalRecordDto?>;
public record GetAlertThresholdQuery(Guid PatientId) : IRequest<AlertThresholdDto?>;
