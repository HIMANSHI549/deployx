# Architecture

## Target system

```
React Dashboard
      |
      v
Spring Boot API  ---- PostgreSQL
      |               Redis (Phase 4)
      |               RabbitMQ (Phase 4)
      v
Deployment Worker --> Docker Runtime --> Reverse Proxy --> user app URL
```

## Phase 1–2 (implemented)

The API authenticates the caller, writes a `deployments` row with status `QUEUED`, and returns `202`-style payload immediately (HTTP 201 with `QUEUED`). An in-process executor picks up the job, appends logs, and transitions status to `BUILDING`, then `READY` or `FAILED`.

The HTTP thread never runs `docker build`.

## Request path

1. `POST /api/v1/deployments` with a project id.
2. Ownership check (`project.owner_id = current user`).
3. Insert deployment `QUEUED`.
4. Audit `DEPLOYMENT_QUEUED`.
5. Submit `DeploymentJob` to the worker.
6. Worker updates status and logs in separate transactions.
7. Frontend polls `GET /api/v1/deployments/{id}`.

## Status machine

`QUEUED → BUILDING → READY`

`QUEUED → BUILDING → FAILED`

`READY → STOPPED` (Phase 3)

`READY → SUPERSEDED` on rollback (Phase 3)

## Isolation notes (Phase 3+)

User containers get memory/CPU limits, a dedicated bridge network, and no Docker socket mount. Repository URLs are restricted to `https://` Git hosts. Builds run as a non-root worker user where the host allows it.
