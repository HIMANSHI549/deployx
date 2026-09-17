# Deploying DeployX

## Local

1. `docker compose -f infra/docker-compose.yml up -d postgres`
2. Run backend with Maven Wrapper
3. Run frontend with Vite

Set `DEPLOYX_WORKER_MODE=docker` only when the API process can talk to a Docker daemon and `git` is on the PATH.

## Phase 6 (planned)

- One Linux VM
- Caddy or Nginx TLS termination
- Compose for Postgres, Redis, RabbitMQ, API, worker, dashboard
- Secrets via environment files with restricted permissions
- Daily `pg_dump` to object storage or a second disk

Do not expose Docker socket to user workloads.
