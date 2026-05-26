using System.Text;
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
using RPM.API.Options;
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
builder.Services.AddControllers();
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
            ValidateLifetime = true, ClockSkew = TimeSpan.Zero
        };
        // Support SignalR JWT from query string
        options.Events = new JwtBearerEvents
        {
            OnMessageReceived = ctx =>
            {
                var accessToken = ctx.Request.Query["access_token"];
                var path = ctx.HttpContext.Request.Path;
                if (!string.IsNullOrEmpty(accessToken) && path.StartsWithSegments("/hubs"))
                    ctx.Token = accessToken;
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

    var bootstrapAdmin = builder.Configuration.GetSection("BootstrapAdmin").Get<BootstrapAdminOptions>();
    if (bootstrapAdmin?.Enabled == true)
    {
        if (string.IsNullOrWhiteSpace(bootstrapAdmin.FullName) ||
            string.IsNullOrWhiteSpace(bootstrapAdmin.Email) ||
            string.IsNullOrWhiteSpace(bootstrapAdmin.Password))
        {
            throw new InvalidOperationException("BootstrapAdmin is enabled but required values are missing.");
        }

        var adminEmail = bootstrapAdmin.Email.Trim().ToLowerInvariant();
        var admin = await db.Users.FirstOrDefaultAsync(u => u.Email == adminEmail);
        if (admin is null)
        {
            admin = User.Create(
                bootstrapAdmin.FullName.Trim(),
                adminEmail,
                bootstrapAdmin.Phone?.Trim() ?? string.Empty,
                hasher.Hash(bootstrapAdmin.Password),
                UserRole.Admin);
            await db.Users.AddAsync(admin);
        }
        else
        {
            admin.UpdateProfile(
                bootstrapAdmin.FullName.Trim(),
                bootstrapAdmin.Phone?.Trim() ?? string.Empty);
            admin.UpdateRole(UserRole.Admin);
            admin.Activate();
            admin.UpdatePasswordHash(hasher.Hash(bootstrapAdmin.Password));
            db.Users.Update(admin);
        }

        await db.SaveChangesAsync();
    }
}

// Middleware pipeline
app.UseMiddleware<GlobalExceptionMiddleware>();
app.UseMiddleware<CorrelationIdMiddleware>();
app.UseSerilogRequestLogging();

if (app.Environment.IsDevelopment())
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
