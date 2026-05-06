using MediatR;
using Microsoft.Extensions.Logging;
namespace RPM.Application.Common.Behaviors;
public class LoggingBehavior<TRequest, TResponse>(ILogger<LoggingBehavior<TRequest, TResponse>> logger)
    : IPipelineBehavior<TRequest, TResponse>
    where TRequest : notnull
{
    public async Task<TResponse> Handle(TRequest request, RequestHandlerDelegate<TResponse> next, CancellationToken ct)
    {
        var name = typeof(TRequest).Name;
        logger.LogInformation("Handling {RequestName}", name);
        var response = await next(ct);
        logger.LogInformation("Handled {RequestName}", name);
        return response;
    }
}
