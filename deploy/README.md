# Deploy remote-care.tech

## Fix "502 Bad Gateway" on register/login

A **502** means nginx is up but the **RPM API is not reachable** on port `8080`.

On your Ubuntu server:

```bash
cd /path/to/Remote-Patiant-Monitoring/docker
docker compose up -d
docker compose ps          # rpm_api should be "running"
curl -s http://127.0.0.1:8080/health   # expect: {"status":"ok"}
```

Then configure nginx (see `nginx-remote-care.tech.conf`) and reload:

```bash
sudo nginx -t && sudo systemctl reload nginx
curl -s https://remote-care.tech/health   # expect: {"status":"ok"}
```

## Checklist

1. `docker compose up -d` in `docker/` (postgres, redis, rpm_api, …)
2. API listens on host `8080` (`docker-compose.yml` maps `8080:8080`)
3. nginx `proxy_pass` points to `http://127.0.0.1:8080` for `/api/` and `/hubs/`
4. Change `Jwt__Secret` in `docker-compose.yml` before production use
5. Open firewall port **1883** for the watch MQTT broker (Mosquitto)

## Watch → website (no live vitals)

1. On the watch: tap **Start** and confirm **Server: connected**.
2. Note the **Patient ID** on the watch (debug default: `ABC123`).
3. On the website dashboard: log in, enter the same Patient ID, click **Connect**.
4. On the server, verify the API ingests MQTT:

```bash
docker compose -f docker/docker-compose.yml logs rpm_api --tail 50 | grep -E "MQTT|Ingested vitals"
docker compose -f docker/docker-compose.yml ps   # rpm_api + mosquitto must be running
```

If the watch shows connected but logs never show `Ingested vitals`, restart the stack:

```bash
cd docker && docker compose restart mosquitto rpm_api
```
