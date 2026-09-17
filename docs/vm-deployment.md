# VM deployment

DeployX is ready for a single Linux VM with Docker and Compose.

## Prerequisites

- Ubuntu 24.04 or another supported Linux distribution
- Docker Engine and Compose plugin
- A DNS record pointing the platform hostname to the VM
- Firewall access for ports 80 and 443 only

## Deploy

```bash
git clone <your-github-repository>
cd deployx
cp .env.example .env
$EDITOR .env
DEPLOYX_JWT_SECRET='<random-32-byte-secret>'
GRAFANA_ADMIN_PASSWORD='<strong-password>'
docker compose -f infra/docker-compose.yml up -d --build
```

The edge proxy listens on port 8081 in the development Compose file. For production, bind it to `80:80`, put TLS termination in front of it with Caddy or Certbot, and do not expose PostgreSQL, Redis, RabbitMQ, or Grafana publicly.

## Operations

```bash
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml logs -f api
docker compose -f infra/docker-compose.yml pull
docker compose -f infra/docker-compose.yml up -d --build
```

Back up PostgreSQL before upgrades and rotate `DEPLOYX_JWT_SECRET` only with a planned token invalidation window.
