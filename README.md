# Remote Healthcare (RPM)

Remote Patient Monitoring platform: wearable vitals ingestion, real-time alerts, doctor–patient workflows, and mobile/web clients backed by an ASP.NET Core API.

## Repository layout

| Path | Description |
|------|-------------|
| `backend/` | ASP.NET Core API (.NET 10), Clean Architecture |
| `docker/` | PostgreSQL, Redis, MQTT, MinIO, API compose stack |
| `web-app/` | React + Vite marketing / admin web app |
| `web-static/` | Static landing assets |
| `android/` | Kotlin Compose doctor/patient mobile app |
| `watchapp/` | Samsung Galaxy Watch vitals publisher (MQTT) |

### Backend solution structure

The solution file is `backend/RPM.slnx` with four projects — each included once:

```
backend/src/
├── RPM.API/            HTTP controllers, SignalR hubs, middleware
├── RPM.Application/    MediatR commands/queries, DTOs, interfaces
├── RPM.Domain/         Entities, enums, repository contracts
└── RPM.Infrastructure/ EF Core, JWT, MQTT, Redis, external services
```

**Controllers** live in a single folder: `backend/src/RPM.API/Controllers/` (8 controllers). SDK-style `.csproj` files use default glob compilation — there are no duplicate controller paths or explicit `<Compile Include>` entries. If your IDE shows duplicate controller entries, re-open the workspace at the repo root (`Remote-Healthcare/`) rather than a nested subfolder.

## Quick start

### 1. Infrastructure

```bash
cd docker
docker compose up -d
```

See [docker/README.md](docker/README.md) for service URLs and credentials.

### 2. Backend API

```bash
cd backend
dotnet ef database update --project src/RPM.Infrastructure --startup-project src/RPM.API
dotnet run --project src/RPM.API
```

- Swagger: http://localhost:8080/swagger  
- Migrations also run automatically on API startup in Docker.

### 3. Web app

```bash
cd web-app
npm install && npm run dev
```

See [web-app/README.md](web-app/README.md) for build/deploy steps.

### 4. Watch app

See [watchapp/README.md](watchapp/README.md) for MQTT broker settings and payload format.

## Architecture

```
┌─────────────┐   MQTT    ┌──────────────┐   REST/WS   ┌──────────────┐
│ Galaxy Watch│ ────────► │  RPM API     │ ◄────────── │ Android/Web  │
└─────────────┘           │  + SignalR   │             └──────────────┘
                          └──────┬───────┘
                    PostgreSQL │ Redis │ MinIO
```

- **Vitals ingestion**: MQTT background service → MediatR handlers → PostgreSQL + Redis cache + SignalR broadcast.
- **Auth**: JWT access tokens (`IJwtService`) + opaque refresh tokens stored as hashes (`IRefreshTokenService`).
- **Alerts**: Threshold evaluation on new vitals; FCM push via Firebase.

## Authentication

| Token | Type | Storage | Validation |
|-------|------|---------|------------|
| Access | JWT | Client memory/storage | `JwtBearer` middleware in `Program.cs` |
| Refresh | Opaque random string | Hashed in `RefreshTokens` table | `IRefreshTokenService.FindActiveAsync` |

Endpoints:

- `POST /api/auth/login` — issue access + refresh tokens
- `POST /api/auth/refresh` — rotate refresh token, return new pair
- `POST /api/auth/logout` — revoke refresh token (requires auth)

## Configuration

Key environment variables (see `docker/docker-compose.yml`):

| Variable | Purpose |
|----------|---------|
| `ConnectionStrings__DefaultConnection` | PostgreSQL |
| `ConnectionStrings__Redis` | Redis cache + SignalR backplane |
| `Jwt__Secret` | JWT signing key (≥ 32 chars in production) |
| `Jwt__ExpiryHours` | Access token lifetime |

## Further reading

- [docker/README.md](docker/README.md) — infrastructure services
- [watchapp/README.md](watchapp/README.md) — watch → server connection
- [web-app/README.md](web-app/README.md) — frontend development
