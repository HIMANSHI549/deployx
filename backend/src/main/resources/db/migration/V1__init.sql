-- DeployX V1 schema

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id),
    name VARCHAR(120) NOT NULL,
    repo_url TEXT NOT NULL,
    branch VARCHAR(200) NOT NULL DEFAULT 'main',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_projects_owner_name UNIQUE (owner_id, name)
);

CREATE INDEX idx_projects_owner ON projects (owner_id);

CREATE TABLE deployments (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    commit_sha VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    image_tag VARCHAR(255),
    public_url VARCHAR(512),
    host_port INTEGER,
    error_message TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deployments_project ON deployments (project_id, created_at DESC);
CREATE INDEX idx_deployments_status ON deployments (status);

CREATE TABLE deployment_logs (
    id BIGSERIAL PRIMARY KEY,
    deployment_id UUID NOT NULL REFERENCES deployments (id) ON DELETE CASCADE,
    level VARCHAR(16) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deployment_logs_deployment ON deployment_logs (deployment_id, created_at);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users (id),
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64),
    resource_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user ON audit_logs (user_id, created_at DESC);
