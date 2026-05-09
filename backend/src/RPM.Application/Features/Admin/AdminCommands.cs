using MediatR;
using RPM.Application.DTOs.Admin;

namespace RPM.Application.Features.Admin;

public record CreateUserAdminCommand(string FullName, string Email, string Phone, string Password, string Role) : IRequest<UserAdminDto>;
public record GetAllAdminUsersQuery() : IRequest<IEnumerable<UserAdminDto>>;
public record UpdateUserAdminCommand(Guid UserId, string FullName, string Phone, string Role, bool IsActive) : IRequest<UserAdminDto>;
public record DeleteUserAdminCommand(Guid UserId) : IRequest;