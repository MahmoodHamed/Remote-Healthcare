# Remote Patient Monitoring - Docker Infrastructure

## Quick Start

```bash
cd docker
docker compose up -d
```

## Services

| Service      | URL / Port        | Credentials               |
|--------------|-------------------|---------------------------|
| RPM API      | http://localhost:8080 | –                       |
| API Swagger  | http://localhost:8080/swagger | –               |
| PostgreSQL   | localhost:5432    | rpm_user / rpm_password   |
| Redis        | localhost:6379    | password: rpm_redis_password |
| MQTT (plain) | localhost:1883    | anonymous (dev mode)      |
| MQTT (WS)    | localhost:9001    | anonymous (dev mode)      |
| MinIO API    | http://localhost:9000 | rpm_minio_access / rpm_minio_secret |
| MinIO Console| http://localhost:9001 | same                   |
| Seq Logs     | http://localhost:5341 | –                       |

## Apply EF Core Migrations

```bash
# From inside the backend folder
dotnet ef database update --project src/RPM.Infrastructure --startup-project src/RPM.API
```

Or on container startup, the API applies migrations automatically via `MigrateDatabaseAsync()` in Program.cs.

## Production Notes

1. **Change `Jwt__Secret`** in `docker-compose.yml` to a strong random string (≥ 32 chars).  
2. **Enable MQTT TLS** (port 8883) by generating certs and updating `mosquitto.conf`.  
3. **Add Firebase credential** path as an environment variable or Docker secret.  
4. MinIO's port 9001 conflicts with Mosquitto WebSocket port — adjust if running both publicly.
