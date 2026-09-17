# Database

PostgreSQL is the source of truth. Schema is applied with Flyway (`backend/src/main/resources/db/migration`).

Externally exposed ids are UUIDs.

## Tables (V1)

- `users` — email unique, BCrypt `password_hash`, `role`
- `projects` — owned by a user; unique `(owner_id, name)`
- `deployments` — status, image tag, public URL, host port, `version` for optimistic locking
- `deployment_logs` — append-only build/runtime lines
- `audit_logs` — security-relevant actions

Indexes exist on foreign keys and `deployments.status`.

## Practices used in code

- Foreign keys with `ON DELETE CASCADE` for deployment children
- Unique constraints on email and project name per owner
- Pagination on list endpoints (`page`, `size`)
- `@Version` on `deployments` for concurrent status updates
- HikariCP connection pool via Spring Boot defaults
