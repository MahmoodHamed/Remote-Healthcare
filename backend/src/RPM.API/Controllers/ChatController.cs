using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RPM.Application.Features.Chat.Commands;
using RPM.Application.Features.Chat.Queries;
using RPM.Application.Common.Interfaces;
namespace RPM.API.Controllers;

[ApiController]
[Route("api/chat")]
[Authorize]
public class ChatController(IMediator mediator, ICurrentUser currentUser) : ControllerBase
{
    [HttpGet("conversations")]
    public async Task<IActionResult> GetConversations(CancellationToken ct) =>
        Ok(await mediator.Send(new GetMyConversationsQuery(currentUser.UserId), ct));

    [HttpPost("conversations")]
    public async Task<IActionResult> CreateConversation([FromBody] CreateConversationCommand cmd, CancellationToken ct) =>
        Ok(await mediator.Send(cmd, ct));

    [HttpGet("conversations/{conversationId}/messages")]
    public async Task<IActionResult> GetMessages(Guid conversationId,
        [FromQuery] int page = 1, [FromQuery] int pageSize = 50, CancellationToken ct = default) =>
        Ok(await mediator.Send(new GetConversationMessagesQuery(conversationId, page, pageSize), ct));

    [HttpPost("conversations/{conversationId}/messages")]
    public async Task<IActionResult> SendMessage(Guid conversationId,
        [FromBody] SendMessageCommand cmd, CancellationToken ct) =>
        Ok(await mediator.Send(cmd with { ConversationId = conversationId, SenderId = currentUser.UserId }, ct));

    [HttpDelete("messages/{messageId}")]
    public async Task<IActionResult> DeleteMessage(Guid messageId, CancellationToken ct)
    {
        await mediator.Send(new DeleteMessageCommand(messageId, currentUser.UserId), ct);
        return NoContent();
    }
}
