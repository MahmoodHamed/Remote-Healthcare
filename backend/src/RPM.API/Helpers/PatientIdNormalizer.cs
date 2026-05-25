using System.Security.Cryptography;
using System.Text;

namespace RPM.API.Helpers;

public static class PatientIdNormalizer
{
    public static Guid? ToGuid(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        if (Guid.TryParse(value, out var guid)) return guid;

        if (value.Length != 6) return null;
        foreach (var ch in value)
        {
            var isDigit = ch >= '0' && ch <= '9';
            var isUpper = ch >= 'A' && ch <= 'Z';
            var isLower = ch >= 'a' && ch <= 'z';
            if (!(isDigit || isUpper || isLower)) return null;
        }

        var bytes = MD5.HashData(Encoding.UTF8.GetBytes(value));
        bytes[6] = (byte)((bytes[6] & 0x0f) | 0x30);
        bytes[8] = (byte)((bytes[8] & 0x3f) | 0x80);
        return Guid.Parse(FormatGuidString(bytes));
    }

    public static string VitalsGroupName(Guid patientId) =>
        $"vitals-{patientId:D}".ToLowerInvariant();

    private static string FormatGuidString(byte[] bytes) =>
        string.Create(36, bytes, (span, b) =>
        {
            const string hex = "0123456789abcdef";
            var idx = 0;
            for (var i = 0; i < 16; i++)
            {
                if (idx == 8 || idx == 13 || idx == 18 || idx == 23) span[idx++] = '-';
                var value = b[i];
                span[idx++] = hex[value >> 4];
                span[idx++] = hex[value & 0x0f];
            }
        });
}
