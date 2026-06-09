# ── Build stage ────────────────────────────────────────────────────────────
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

# Copy solution + project files for layer caching
COPY RPM.slnx .
COPY src/RPM.Domain/RPM.Domain.csproj          src/RPM.Domain/
COPY src/RPM.Application/RPM.Application.csproj src/RPM.Application/
COPY src/RPM.Infrastructure/RPM.Infrastructure.csproj src/RPM.Infrastructure/
COPY src/RPM.API/RPM.API.csproj                src/RPM.API/

RUN dotnet restore src/RPM.API/RPM.API.csproj

# Copy all source code
COPY . .

# Publish release build
RUN dotnet publish src/RPM.API/RPM.API.csproj \
    -c Release \
    -o /app/publish \
    --no-restore

# ── Runtime stage ──────────────────────────────────────────────────────────
FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app

# Create non-root user
RUN groupadd --system appuser \
    && useradd --system --gid appuser --home /app --shell /usr/sbin/nologin appuser \
    && chown -R appuser /app
USER appuser

COPY --from=build /app/publish .

EXPOSE 8080
ENTRYPOINT ["dotnet", "RPM.API.dll"]
