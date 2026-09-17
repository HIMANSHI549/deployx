# DeployX

A mini cloud deployment platform: connect a GitHub repository, build a Docker image, run the container, and open the app on a public URL.

This repository is an SDE portfolio project covering backend engineering, databases, Docker, asynchronous jobs, and production-style architecture. Complexity is added in phases. Each phase has a working deliverable before the next one starts.

## Current status

**Phase 3 slice (this checkout):** sign up / log in, create a project from a public Git repository, queue a deployment, inspect persisted logs, and stop a ready deployment. The in-process worker supports simulated deployments by default and Docker builds with free host-port allocation plus an HTTP readiness probe.

## Tech stack

| Layer | Choice |
| --- | --- |
| Backend | Java 21+, Spring Boot, Maven |
| Frontend | React, TypeScript, Vite |
| Database | PostgreSQL + Flyway |
| Local infra | Docker Compose (Postgres, Redis, RabbitMQ) |
| Auth | JWT + BCrypt |
| Docs | OpenAPI / Swagger UI |

Redis, RabbitMQ, a separate worker process, reverse proxy routing, Prometheus, and Grafana are wired into the target architecture and Compose file. They are **not** required to run Phase 1–2.

## Quick start

### 1. Start local services

```bash
cd infra
docker compose up -d postgres
```

The Compose file also defines Redis and RabbitMQ for later worker extraction:

```bash
docker compose up -d
```

If Docker is not available, install PostgreSQL locally and create database `deployx` with user `deployx` / password `deployx`.

### 2. Run the API

Maven is not required on the PATH. Use the wrapper after the wrapper JAR is present (see `backend/README.md`).

```bash
cd backend
./mvnw spring-boot:run
```

Windows:

```bat
cd backend
mvnw.cmd spring-boot:run
```

API: [http://localhost:8080](http://localhost:8080)  
Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### 3. Run the dashboard

```bash
cd frontend
npm install
npm run dev
```

UI: [http://localhost:5173](http://localhost:5173)

### 4. Try a deployment

1. Register an account.
2. Create a project. Use a public HTTPS Git URL (for example the bundled `examples/node-demo` after you push it, or any public repo that contains a `Dockerfile`).
3. Click **Deploy**. The API returns immediately with status `QUEUED`.
4. Refresh the deployment. Status should become `READY` or `FAILED`, with logs.
5. Open a ready deployment URL or use **Stop** to remove its container.

## Repository layout

```
deployx/
├── backend/          Spring Boot API (worker lives here until Phase 4)
├── frontend/         React dashboard
├── worker/           Placeholder for the extracted deployment worker
├── infra/            Compose, Nginx, monitoring
├── examples/         Sample apps that DeployX can build
├── docs/             Architecture, API, database, deployment
└── .github/workflows CI
```

## Implementation roadmap

### Phase 1 — Foundation (week 1)

**Deliverable:** authenticated users can create projects backed by PostgreSQL.

- Monorepo, Compose Postgres, Flyway schema
- Register / login (JWT)
- CRUD-lite for projects (create + list + get)
- OpenAPI, global error handling, audit log for important actions

### Phase 2 — Async deployments (week 2)

**Deliverable:** `POST /api/v1/deployments` queues work; HTTP does not wait for the build.

- Deployment records and status machine
- In-process worker (`@Async`)
- Deployment logs persisted and listed by the UI
- Optimistic locking on deployment status updates

### Phase 3 — Real Docker runtime (week 3)

**Deliverable:** a container runs and is reachable on a URL.

- Clone branch, require `Dockerfile`, `docker build` / `docker run`
- Resource limits, HTTP readiness check, ephemeral host port allocation
- Stop endpoint for ready deployments
- Nginx/Caddy hostname routing
- Rollback to a previous `READY` deployment

### Phase 4 — Distributed worker (week 4)

**Deliverable:** API and worker are separate processes.

- RabbitMQ job queue
- Redis for job state / rate limiting
- Extract `worker/` from the backend
- Idempotent consumers

### Phase 5 — Quality and operations (week 5)

**Deliverable:** CI runs tests; metrics are visible.

- JUnit + Mockito + Testcontainers
- Vitest + React Testing Library
- GitHub Actions
- Prometheus + Grafana

### Phase 6 — Production on a VM (week 6)

**Deliverable:** DeployX itself is deployed; demo apps get stable URLs.

- Linux VM, TLS, secrets, backups
- Production Compose or systemd units
- Hardening (authz, repo URL validation, container isolation)

## What not to do yet

Do not start with Kubernetes, a custom orchestrator, or GitHub App OAuth until Phase 3 works with public HTTPS repos and Docker. The worker stays inside Spring Boot until queuing and status updates are correct.

## License

Educational / portfolio use.
