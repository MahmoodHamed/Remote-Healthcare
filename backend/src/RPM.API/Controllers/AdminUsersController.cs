using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.Common.Interfaces;
using RPM.Application.DTOs.Admin;
using RPM.Application.Features.Admin;

namespace RPM.API.Controllers;

[ApiController]
[Route("api/admin/users")]
[Authorize(Roles = "Admin")]
public class AdminUsersController(IMediator mediator, IAuditService audit, ICurrentUser currentUser) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetAll(CancellationToken ct) =>
        Ok(await mediator.Send(new GetAllAdminUsersQuery(), ct));

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] UpsertUserAdminRequest request, CancellationToken ct)
    {
        var result = await mediator.Send(new CreateUserAdminCommand(request.FullName, request.Email, request.Phone, request.Password, request.Role), ct);
        await audit.LogAsync(new(currentUser.UserId, currentUser.Email, "AdminCreateUser", "User", $"{request.Email} role={request.Role}", HttpContext.Connection.RemoteIpAddress?.ToString()), ct);
        return Ok(result);
    }

    [HttpPut("{userId:guid}")]
    public async Task<IActionResult> Update(Guid userId, [FromBody] UpdateUserAdminRequest request, CancellationToken ct)
    {
        var result = await mediator.Send(new UpdateUserAdminCommand(userId, request.FullName, request.Phone, request.Role, request.IsActive), ct);
        await audit.LogAsync(new(currentUser.UserId, currentUser.Email, "AdminUpdateUser", "User", $"userId={userId} role={request.Role} active={request.IsActive}", HttpContext.Connection.RemoteIpAddress?.ToString()), ct);
        return Ok(result);
    }

    [HttpDelete("{userId:guid}")]
    public async Task<IActionResult> Delete(Guid userId, CancellationToken ct)
    {
        await mediator.Send(new DeleteUserAdminCommand(userId), ct);
        await audit.LogAsync(new(currentUser.UserId, currentUser.Email, "AdminDeleteUser", "User", $"userId={userId}", HttpContext.Connection.RemoteIpAddress?.ToString()), ct);
        return NoContent();
    }
}