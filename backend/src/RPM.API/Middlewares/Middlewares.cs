using System.Net;
using System.Text.Json;
using RPM.Application.Common.Exceptions;
namespace RPM.API.Middlewares;

public class GlobalExceptionMiddleware(RequestDelegate next, ILogger<GlobalExceptionMiddleware> logger)
{
    public async Task InvokeAsync(HttpContext context)
    {
        try { await next(context); }
        catch (Exception ex) { await HandleExceptionAsync(context, ex, logger); }
    }

    private static async Task HandleExceptionAsync(HttpContext ctx, Exception ex, ILogger logger)
    {
        logger.LogError(ex, "Unhandled exception: {Message}", ex.Message);

        var (status, title, errors) = ex switch
        {
            NotFoundException => (HttpStatusCode.NotFound, "Not Found", null as object),
            ValidationException ve => (HttpStatusCode.BadRequest, "Validation Failed", ve.Errors),
            ConflictException => (HttpStatusCode.Conflict, "Conflict", null as object),
            UnauthorizedException => (HttpStatusCode.Unauthorized, "Unauthorized", null as object),
            ForbiddenException => (HttpStatusCode.Forbidden, "Forbidden", null as object),
            _ => (HttpStatusCode.InternalServerError, "An unexpected error occurred.", null as object)
        };

        ctx.Response.ContentType = "application/json";
        ctx.Response.StatusCode = (int)status;

        var response = new { title, status = (int)status, errors, traceId = ctx.TraceIdentifier };
        await ctx.Response.WriteAsync(JsonSerializer.Serialize(response, new JsonSerializerOptions { PropertyNamingPolicy = JsonNamingPolicy.CamelCase }));
    }
}

public class CorrelationIdMiddleware(RequestDelegate next)
{
    private const string Header = "X-Correlation-ID";
    public async Task InvokeAsync(HttpContext context)
    {
        if (!context.Request.Headers.ContainsKey(Header))
            context.Request.Headers[Header] = Guid.NewGuid().ToString();
        context.Response.Headers[Header] = context.Request.Headers[Header];
        await next(context);
    }
}
