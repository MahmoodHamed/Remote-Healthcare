using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.Common.Interfaces;
using RPM.Application.Features.Auth.Commands;
namespace RPM.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController(IMediator mediator, IAuditService audit, ICurrentUser currentUser) : ControllerBase
{
    /// <summary>Legacy single-endpoint registration. Prefer the role-specific endpoints.</summary>
    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterCommand cmd, CancellationToken ct)
    {
        var result = await mediator.Send(cmd, ct);
        await audit.LogAsync(new(null, cmd.Email, "Register", "User", cmd.Email, HttpContext.Connection.RemoteIpAddress?.ToString()), ct);
        return Ok(result);
    }

    [HttpPost("register/patient")]
    public async Task<IActionResult> RegisterPatient([FromBody] RegisterPatientCommand cmd, CancellationToken ct)
    {
        var result = await mediator.Send(cmd, ct);
        await audit.LogAsync(new(null, cmd.Email, "RegisterPatient", "User", cmd.Email, HttpContext.Connection.RemoteIpAddress?.ToString()), ct);
        return Ok(result);
    }

    [HttpPost("register/doctor")]
    public async Task<IActionResult> RegisterDoctor([FromBody] RegisterDoctorCommand cmd, CancellationToken ct)
    {
        var result = await mediator.Send(cmd, ct);
        await audit.LogAsync(new(null, cmd.Email, "RegisterDoctor", "User", cmd.Email, HttpContext.Connection.RemoteIpAddress?.ToString()), ct);
        return Ok(result);
    }

    /// <summary>Login for patient, doctor and relative roles.</summary>
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginCommand cmd, CancellationToken ct)
    {
        var result = await mediator.Send(cmd, ct);
        await audit.LogAsync(new(null, cmd.Email, "Login", "Auth", null, HttpContext.Connection.RemoteIpAddress?.ToString()), ct);
        return Ok(result);
    }

    /// <summary>Dedicated admin sign-in endpoint. Rejects non-admin accounts.</summary>
    [HttpPost("admin/login")]
    public async Task<IActionResult> AdminLogin([FromBody] AdminLoginCommand cmd, CancellationToken ct)
    {
        var result = await mediator.Send(cmd, ct);
        await audit.LogAsync(new(null, cmd.Email, "AdminLogin", "Auth", null, HttpContext.Connection.RemoteIpAddress?.ToString()), ct);
        return Ok(result);
    }

    [HttpPost("refresh")]
    public async Task<IActionResult> Refresh([FromBody] RefreshTokenCommand cmd, CancellationToken ct) =>
        Ok(await mediator.Send(cmd, ct));

    [HttpPost("logout")]
    [Authorize]
    public async Task<IActionResult> Logout([FromBody] LogoutCommand cmd, CancellationToken ct)
    {
        await mediator.Send(cmd, ct);
        await audit.LogAsync(new(currentUser.UserId, currentUser.Email, "Logout", "Auth", null, HttpContext.Connection.RemoteIpAddress?.ToString()), ct);
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
