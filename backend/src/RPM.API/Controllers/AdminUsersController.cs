using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.DTOs.Admin;
using RPM.Application.Features.Admin;

namespace RPM.API.Controllers;

[ApiController]
[Route("api/admin/users")]
[Authorize(Roles = "Admin")]
public class AdminUsersController(IMediator mediator) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetAll(CancellationToken ct)
    {
        return Ok(await mediator.Send(new GetAllAdminUsersQuery(), ct));
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] UpsertUserAdminRequest request, CancellationToken ct)
        => Ok(await mediator.Send(new CreateUserAdminCommand(request.FullName, request.Email, request.Phone, request.Password, request.Role), ct));

    [HttpPut("{userId:guid}")]
    public async Task<IActionResult> Update(Guid userId, [FromBody] UpdateUserAdminRequest request, CancellationToken ct)
        => Ok(await mediator.Send(new UpdateUserAdminCommand(userId, request.FullName, request.Phone, request.Role, request.IsActive), ct));

    [HttpDelete("{userId:guid}")]
    public async Task<IActionResult> Delete(Guid userId, CancellationToken ct)
    {
        await mediator.Send(new DeleteUserAdminCommand(userId), ct);
        return NoContent();
    }
}