namespace RPM.Application.Features.Vitals;

/// <summary>
/// Samsung off-body detection often reports false while vitals are still streaming.
/// Prefer live sensor evidence over the raw isWearing flag from the watch.
/// </summary>
public static class VitalWearingInference
{
    public static bool Infer(
        bool reportedWearing,
        float? heartRateBpm,
        float? spO2Percent,
        float? temperatureC,
        float? skinTemperatureC,
        float? ambientTemperatureC,
        float? hrvMs,
        float? stressScore,
        float? bodyFatPercent,
        float? ecgAvgHeartRateBpm,
        int? stepsCount,
        float? caloriesBurned)
    {
        if (HasLiveEvidence(heartRateBpm, spO2Percent, temperatureC, skinTemperatureC,
                ambientTemperatureC, hrvMs, stressScore, bodyFatPercent, ecgAvgHeartRateBpm,
                stepsCount, caloriesBurned))
            return true;

        if (reportedWearing) return true;
        return false;
    }

    private static bool HasLiveEvidence(
        float? heartRateBpm,
        float? spO2Percent,
        float? temperatureC,
        float? skinTemperatureC,
        float? ambientTemperatureC,
        float? hrvMs,
        float? stressScore,
        float? bodyFatPercent,
        float? ecgAvgHeartRateBpm,
        int? stepsCount,
        float? caloriesBurned) =>
        heartRateBpm is >= 30f
        || spO2Percent.HasValue
        || temperatureC.HasValue
        || skinTemperatureC.HasValue
        || ambientTemperatureC.HasValue
        || hrvMs.HasValue
        || stressScore.HasValue
        || bodyFatPercent.HasValue
        || ecgAvgHeartRateBpm.HasValue
        || stepsCount is > 0
        || caloriesBurned is > 0f;
}
