using System.Security.Cryptography;
using System.Text;
using RPM.Application.Common.Interfaces;

namespace RPM.Infrastructure.Services;

public class TokenHasher : ITokenHasher
{
    public string HashToken(string token)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(token));
        return Convert.ToBase64String(bytes);
    }
}
