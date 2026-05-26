using MediatR;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Auth;
using RPM.Application.Features.Auth.Commands;
using RPM.Application.Common.Exceptions;
using RPM.Domain.Entities;
using RPM.Domain.Interfaces;

namespace RPM.Application.Features.Auth.Handlers;

public class RefreshTokenCommandHandler(IUnitOfWork uow, IPasswordHasher hasher, IJwtService jwt)
    : IRequestHandler<RefreshTokenCommand, AuthTokensDto>
{
    public async Task<AuthTokensDto> Handle(RefreshTokenCommand cmd, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(cmd.RefreshToken))
            throw new UnauthorizedException("Refresh token required.");

        // find existing refresh token by comparing hash
        var usersRepo = uow.Users;

        // Since refresh tokens are stored hashed, we must find by iterating or by hash equality
        // We'll compute no hash equality here; instead we search by all tokens and verify
        var allUsers = await usersRepo.GetAllAsync(ct);
        RefreshToken? found = null;
        foreach (var user in allUsers)
        {
            foreach (var rt in user.RefreshTokens)
            {
                if (hasher.Verify(cmd.RefreshToken, rt.TokenHash))
                {
                    found = rt;
                    break;
                }
            }
            if (found != null) break;
        }

        if (found is null || !found.IsActive)
            throw new UnauthorizedException("Invalid or expired refresh token.");

        // rotate tokens: revoke old, add new
        found.Revoke();
        uow.Users.UpdateRefreshToken(found);

        var userId = found.UserId;
        var foundUser = await usersRepo.GetByIdAsync(userId, ct) ?? throw new NotFoundException(nameof(User), userId);

        var newAccess = jwt.GenerateAccessToken(foundUser.Id, foundUser.Email, foundUser.Role.ToString());
        var newRefresh = jwt.GenerateRefreshToken();
        var newRt = RefreshToken.Create(foundUser.Id, hasher.Hash(newRefresh), DateTime.UtcNow.AddDays(30), cmd.DeviceInfo);
        await usersRepo.AddRefreshTokenAsync(newRt, ct);

        await uow.SaveChangesAsync(ct);

        return new AuthTokensDto(newAccess, newRefresh, DateTime.UtcNow.AddHours(1));
    }
}