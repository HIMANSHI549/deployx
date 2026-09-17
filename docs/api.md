# Deployment lifecycle

Authenticated deployment routes:

- `POST /api/v1/projects/{projectId}/deployments` queues a deployment and returns `QUEUED`.
- `GET /api/v1/projects/{projectId}/deployments` lists a user's deployments.
- `GET /api/v1/deployments/{id}` returns the current status.
- `GET /api/v1/deployments/{id}/logs` returns persisted worker logs.
- `POST /api/v1/deployments/{id}/stop` stops a `READY` deployment and returns `204 No Content`.

In Docker mode, the worker allocates an available host port, runs the image with memory and CPU limits, and only marks it `READY` after the configured health path responds. Configure the probe with `DEPLOYX_WORKER_HEALTH_PATH` (default `/`).
# API

Base path: `/api/v1`  
Auth: `Authorization: Bearer <jwt>` except register/login.

Interactive docs: `/swagger-ui.html`

## Auth

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Create account |
| POST | `/api/v1/auth/login` | Issue JWT |
| GET | `/api/v1/auth/me` | Current user |

Register body:

```json
{ "email": "dev@example.com", "password": "at-least-8-chars" }
```

## Projects

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/v1/projects` | Create project |
| GET | `/api/v1/projects` | List own projects (paginated) |
| GET | `/api/v1/projects/{id}` | Get project |

Create body:

```json
{
  "name": "portfolio",
  "repoUrl": "https://github.com/org/repo.git",
  "branch": "main"
}
```

## Deployments

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/v1/projects/{projectId}/deployments` | Queue deployment |
| GET | `/api/v1/projects/{projectId}/deployments` | List deployments |
| GET | `/api/v1/deployments/{id}` | Get deployment |
| GET | `/api/v1/deployments/{id}/logs` | List logs (paginated) |

The create-deployment call returns as soon as the row is persisted and the job is submitted. Poll GET until `READY` or `FAILED`.
