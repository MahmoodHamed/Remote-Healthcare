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
