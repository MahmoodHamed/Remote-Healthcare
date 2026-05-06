using RPM.API.Extensions;
using RPM.Application.Common.Interfaces;
namespace RPM.API;

public static class ApiServiceExtensions
{
    public static IServiceCollection AddApiServices(this IServiceCollection services)
    {
        services.AddScoped<IVitalsHubService, VitalsHubService>();
        return services;
    }
}
