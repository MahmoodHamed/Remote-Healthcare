namespace RPM.Application.Common.Interfaces;

public interface ITokenHasher
{
    string HashToken(string token);
}
