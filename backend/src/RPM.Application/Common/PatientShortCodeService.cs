using RPM.Domain.Entities;
using RPM.Domain.Interfaces;

namespace RPM.Application.Common;

public static class PatientShortCodeService
{
    public static async Task<string> EnsureAssignedAsync(
        PatientProfile profile, IUnitOfWork uow, CancellationToken ct = default)
    {
        if (!string.IsNullOrWhiteSpace(profile.ShortPatientCode))
            return profile.ShortPatientCode!;

        for (var salt = 0; salt < 20; salt++)
        {
            var candidate = PatientShortCode.Generate(profile.UserId, salt);
            if (!await uow.Patients.ShortPatientCodeExistsAsync(candidate, ct))
            {
                profile.AssignShortPatientCode(candidate);
                uow.Patients.Update(profile);
                return candidate;
            }
        }

        var fallback = PatientShortCode.Generate(Guid.NewGuid());
        profile.AssignShortPatientCode(fallback);
        uow.Patients.Update(profile);
        return fallback;
    }
}
