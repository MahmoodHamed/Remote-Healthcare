using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;
using Microsoft.EntityFrameworkCore;
using RPM.Application;
using RPM.Application.Common.Interfaces;
using RPM.Infrastructure;
using RPM.Infrastructure.Persistence;
using RPM.Domain.Entities;
using RPM.Domain.Enums;
using RPM.API;
using RPM.API.Middlewares;
using RPM.API.Hubs;
using Serilog;

var builder = WebApplication.CreateBuilder(args);

// Serilog
Log.Logger = new LoggerConfiguration()
    .ReadFrom.Configuration(builder.Configuration)
    .WriteTo.Console()
    .WriteTo.Seq(builder.Configuration["Seq:Url"] ?? "http://localhost:5341")
    .Enrich.FromLogContext()
    .CreateLogger();
builder.Host.UseSerilog();

// Services
builder.Services.AddControllers()
    .AddJsonOptions(o =>
    {
        o.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        o.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
        o.JsonSerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;
    });
builder.Services.AddEndpointsApiExplorer();

// Swagger with JWT
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo { Title = "RPM API", Version = "v1", Description = "Remote Patient Monitoring API" });
    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        In = ParameterLocation.Header, Description = "JWT Bearer token",
        Name = "Authorization", Type = SecuritySchemeType.Http, BearerFormat = "JWT", Scheme = "bearer"
    });
    c.AddSecurityRequirement(doc =>
        new OpenApiSecurityRequirement
        {
            { new OpenApiSecuritySchemeReference("Bearer", doc, null), [] }
        });
});

// JWT Auth
var jwtSecret = builder.Configuration["Jwt:Secret"] ?? throw new InvalidOperationException("Jwt:Secret missing");
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSecret)),
            ValidateIssuer = true, ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidateAudience = true, ValidAudience = builder.Configuration["Jwt:Audience"],
            ValidateLifetime = true, ClockSkew = TimeSpan.FromMinutes(2)
        };
        // SignalR: Bearer header (negotiate) + access_token query (WebSocket)
        options.Events = new JwtBearerEvents
        {
            OnMessageReceived = ctx =>
            {
                var path = ctx.HttpContext.Request.Path;
                if (!path.StartsWithSegments("/hubs")) return Task.CompletedTask;

                var queryToken = ctx.Request.Query["access_token"];
                if (!string.IsNullOrEmpty(queryToken))
                {
                    ctx.Token = queryToken;
                    return Task.CompletedTask;
                }

                var authHeader = ctx.Request.Headers.Authorization.ToString();
                if (authHeader.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
                    ctx.Token = authHeader["Bearer ".Length..].Trim();

                return Task.CompletedTask;
            }
        };
    });

builder.Services.AddAuthorization();
builder.Services.AddCors(opt =>
    opt.AddPolicy("AllowMobile", p => p.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader()));

// Clean Architecture layers
builder.Services.AddApplication();
builder.Services.AddInfrastructure(builder.Configuration);
builder.Services.AddApiServices();

var app = builder.Build();

// Apply pending EF Core migrations on startup so required tables exist.
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    var hasher = scope.ServiceProvider.GetRequiredService<IPasswordHasher>();
    db.Database.Migrate();

    await db.Database.ExecuteSqlRawAsync("""
        CREATE TABLE IF NOT EXISTS "AuditLogs" (
            "Id"          uuid                     PRIMARY KEY DEFAULT gen_random_uuid(),
            "UserId"      uuid,
            "UserEmail"   text,
            "Action"      text                     NOT NULL,
            "Resource"    text,
            "Detail"      text,
            "IpAddress"   text,
            "OccurredAt"  timestamp with time zone NOT NULL DEFAULT now()
        );
        CREATE INDEX IF NOT EXISTS "IX_AuditLogs_OccurredAt" ON "AuditLogs" ("OccurredAt" DESC);
    """);

    var adminEmail = builder.Configuration["Admin:Email"] ?? "mahmoodjob8@gmail.com";
    var adminPassword = builder.Configuration["Admin:Password"] ?? "M1@a2@h3&m4&";

    var admin = await db.Users.FirstOrDefaultAsync(u => u.Email == adminEmail);
    if (admin is null)
    {
        admin = User.Create("Mahmood Job", adminEmail, "+1000000000", hasher.Hash(adminPassword), UserRole.Admin);
        await db.Users.AddAsync(admin);
    }
    else
    {
        admin.UpdateProfile("Mahmood Job", "+1000000000");
        admin.UpdateRole(UserRole.Admin);
        admin.Activate();
        admin.UpdatePasswordHash(hasher.Hash(adminPassword));
        db.Users.Update(admin);
    }

    await db.SaveChangesAsync();
}

// Middleware pipeline
app.UseMiddleware<GlobalExceptionMiddleware>();
app.UseMiddleware<CorrelationIdMiddleware>();
app.UseSerilogRequestLogging();

var swaggerEnabled = app.Environment.IsDevelopment()
    || builder.Configuration.GetValue<bool>("Swagger:Enabled");

if (swaggerEnabled)
{
    app.UseSwagger();
    app.UseSwaggerUI(c => c.SwaggerEndpoint("/swagger/v1/swagger.json", "RPM API v1"));
}

app.UseHttpsRedirection();
app.UseCors("AllowMobile");
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
app.MapHub<VitalsHub>("/hubs/vitals");
app.MapHub<ChatHub>("/hubs/chat");

app.Run();
