using System.Security.Cryptography;
using System.Text;

namespace RPM.Application.Common;

/// <summary>6-character patient code (A-Z, 0-9) for watch pairing — e.g. ABC123.</summary>
public static class PatientShortCode
{
    private const string Chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static bool IsValidFormat(string? value)
    {
        if (string.IsNullOrWhiteSpace(value) || value.Length != 6) return false;
        foreach (var ch in value)
        {
            if (!char.IsAsciiLetterOrDigit(ch)) return false;
        }
        return true;
    }

    public static string Normalize(string code) => code.Trim().ToUpperInvariant();

    public static string Generate(Guid userId, int salt = 0)
    {
        var bytes = salt == 0
            ? SHA256.HashData(Encoding.UTF8.GetBytes(userId.ToString("D")))
            : SHA256.HashData(Encoding.UTF8.GetBytes($"{userId:D}:{salt}"));

        Span<char> code = stackalloc char[6];
        for (var i = 0; i < 6; i++)
            code[i] = Chars[bytes[i] % Chars.Length];
        return new string(code);
    }
}
