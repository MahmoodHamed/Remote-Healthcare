using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.Common.Interfaces;
using RPM.Application.Features.Auth.Commands;
using RPM.Domain.Interfaces;
namespace RPM.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController(IMediator mediator, IUnitOfWork uow, ICurrentUser currentUser) : ControllerBase
{
    [HttpGet("me")]
    [Authorize]
    public async Task<IActionResult> GetMe(CancellationToken ct)
    {
        var user = await uow.Users.GetByIdAsync(currentUser.UserId, ct);
        if (user is null) return NotFound();
        return Ok(new
        {
            id = user.Id,
            fullName = user.FullName,
            email = user.Email,
            phone = user.Phone,
            role = user.Role.ToString(),
            avatarUrl = user.AvatarUrl
        });
    }
    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterCommand cmd, CancellationToken ct) =>
        Ok(await mediator.Send(cmd, ct));

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginCommand cmd, CancellationToken ct) =>
        Ok(await mediator.Send(cmd, ct));

    [HttpPost("refresh")]
    public async Task<IActionResult> Refresh([FromBody] RefreshTokenCommand cmd, CancellationToken ct) =>
        Ok(await mediator.Send(cmd, ct));

    [HttpPost("logout")]
    [Authorize]
    public async Task<IActionResult> Logout([FromBody] LogoutCommand cmd, CancellationToken ct)
    {
        await mediator.Send(cmd, ct);
        return NoContent();
    }

    [HttpPatch("fcm-token")]
    [Authorize]
    public async Task<IActionResult> UpdateFcmToken([FromBody] UpdateFcmTokenCommand cmd, CancellationToken ct)
    {
        await mediator.Send(cmd, ct);
        return NoContent();
    }
}
