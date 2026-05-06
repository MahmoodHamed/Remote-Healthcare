using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Microsoft.Extensions.Configuration;

namespace RPM.Infrastructure.Persistence;

/// <summary>
/// Used by EF Core tools (dotnet-ef migrations) at design time.
/// Reads connection string from environment variable or falls back to a local dev default.
/// </summary>
public class DesignTimeDbContextFactory : IDesignTimeDbContextFactory<AppDbContext>
{
    public AppDbContext CreateDbContext(string[] args)
    {
        // Try to read from an appsettings.json in the API project (when running from solution root)
        var config = new ConfigurationBuilder()
            .SetBasePath(Path.Combine(Directory.GetCurrentDirectory(), "../RPM.API"))
            .AddJsonFile("appsettings.json", optional: true)
            .AddEnvironmentVariables()
            .Build();

        var connectionString = config.GetConnectionString("DefaultConnection")
            ?? "Host=localhost;Port=5432;Database=rpm_db;Username=rpm_user;Password=rpm_password";

        var optionsBuilder = new DbContextOptionsBuilder<AppDbContext>();
        optionsBuilder.UseNpgsql(connectionString,
            o => o.MigrationsAssembly("RPM.Infrastructure"));

        return new AppDbContext(optionsBuilder.Options);
    }
}
