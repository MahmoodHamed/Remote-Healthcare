using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using RPM.Application.DTOs.Vitals;

namespace RPM.Infrastructure.BackgroundServices;

/// <summary>Parses watch MQTT JSON. Accepts field-name variants from different watch builds.</summary>
public static class MqttVitalsParser
{
    public static VitalIngestionDto? TryParse(string payload)
    {
        if (string.IsNullOrWhiteSpace(payload)) return null;

        using var doc = JsonDocument.Parse(payload);
        var root = doc.RootElement;

        var patientId = NormalizeGuid(GetString(root, "patientId"));
        var deviceId = NormalizeGuid(GetString(root, "deviceId"));
        if (patientId is null || deviceId is null) return null;

        var ambientTemp = GetFloat(root, "ambientTemperatureC", "temperatureC");

        return new VitalIngestionDto(
            patientId.Value,
            deviceId.Value,
            GetFloat(root, "heartRateBpm"),
            GetFloat(root, "spO2Percent", "spo2Percent"),
            GetFloat(root, "systolicBp"),
            GetFloat(root, "diastolicBp"),
            ambientTemp,
            GetInt(root, "stepsCount"),
            GetFloat(root, "caloriesBurned"),
            GetBool(root, "fallDetected") ?? false,
            GetBool(root, "isWearing") ?? true,
            GetFloat(root, "skinTemperatureC"),
            ambientTemp,
            GetFloat(root, "heartRateVariabilityMs", "hrvMs"),
            GetFloat(root, "restingHeartRateBpm"),
            GetFloat(root, "maxHeartRateBpm"),
            GetFloat(root, "respirationRateBpm"),
            GetFloat(root, "distanceMeters"),
            GetInt(root, "floorsClimbed"),
            GetInt(root, "activeMinutes"),
            GetFloat(root, "stressScore"),
            GetFloat(root, "sleepScore"),
            GetInt(root, "sleepDurationMinutes"),
            GetFloat(root, "bodyFatPercent", "bodyFatPct"),
            GetFloat(root, "muscleMassKg"),
            GetFloat(root, "bodyWaterPercent"),
            GetFloat(root, "basalMetabolicRate"),
            GetFloat(root, "ecgAverageHeartRate", "ecgAvgHeartRateBpm"),
            GetString(root, "ecgClassification"),
            GetString(root, "ecgWaveformJson"),
            GetFloat(root, "bloodGlucoseMgDl"),
            GetFloat(root, "batteryLevel"));
    }

    private static float? GetFloat(JsonElement root, params string[] names)
    {
        foreach (var name in names)
        {
            if (!TryGetProperty(root, name, out var value)) continue;
            if (value.ValueKind == JsonValueKind.Number && value.TryGetSingle(out var number)) return number;
        }
        return null;
    }

    private static int? GetInt(JsonElement root, params string[] names)
    {
        foreach (var name in names)
        {
            if (!TryGetProperty(root, name, out var value)) continue;
            if (value.ValueKind == JsonValueKind.Number && value.TryGetInt32(out var number)) return number;
        }
        return null;
    }

    private static bool? GetBool(JsonElement root, string name)
    {
        if (!TryGetProperty(root, name, out var value)) return null;
        return value.ValueKind switch
        {
            JsonValueKind.True => true,
            JsonValueKind.False => false,
            _ => null
        };
    }

    private static string? GetString(JsonElement root, string name)
    {
        if (!TryGetProperty(root, name, out var value)) return null;
        return value.ValueKind == JsonValueKind.String ? value.GetString() : null;
    }

    private static bool TryGetProperty(JsonElement root, string name, out JsonElement value)
    {
        if (root.TryGetProperty(name, out value)) return true;

        foreach (var prop in root.EnumerateObject())
        {
            if (string.Equals(prop.Name, name, StringComparison.OrdinalIgnoreCase))
            {
                value = prop.Value;
                return true;
            }
        }

        value = default;
        return false;
    }

    private static Guid? NormalizeGuid(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        if (Guid.TryParse(value, out var guid)) return guid;

        if (!IsShortId(value)) return null;

        var bytes = MD5.HashData(Encoding.UTF8.GetBytes(value));
        bytes[6] = (byte)((bytes[6] & 0x0f) | 0x30);
        bytes[8] = (byte)((bytes[8] & 0x3f) | 0x80);
        return Guid.Parse(FormatGuidString(bytes));
    }

    private static bool IsShortId(string value)
    {
        if (value.Length != 6) return false;
        foreach (var ch in value)
        {
            var isDigit = ch is >= '0' and <= '9';
            var isUpper = ch is >= 'A' and <= 'Z';
            var isLower = ch is >= 'a' and <= 'z';
            if (!(isDigit || isUpper || isLower)) return false;
        }
        return true;
    }

    private static string FormatGuidString(byte[] bytes) =>
        string.Create(36, bytes, (span, b) =>
        {
            var hex = "0123456789abcdef";
            var map = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
            var idx = 0;
            for (var i = 0; i < map.Length; i += 1)
            {
                if (idx is 8 or 13 or 18 or 23) span[idx++] = '-';
                var v = b[map[i]];
                span[idx++] = hex[v >> 4];
                span[idx++] = hex[v & 0x0f];
            }
        });
}
