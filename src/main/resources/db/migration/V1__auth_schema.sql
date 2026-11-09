CREATE SCHEMA IF NOT EXISTS authz;

CREATE TABLE authz.roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(64) NOT NULL UNIQUE,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(255),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by  VARCHAR(255)
);

CREATE TABLE authz.permissions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code              VARCHAR(128) NOT NULL UNIQUE,
    permission_group  VARCHAR(64)  NOT NULL,
    action_code       VARCHAR(32)  NOT NULL,
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        VARCHAR(255),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by        VARCHAR(255)
);
CREATE UNIQUE INDEX uq_authz_permissions_group_action ON authz.permissions (permission_group, action_code);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(64)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    data_scope      VARCHAR(128),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(255),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(255)
);

CREATE TABLE authz.user_roles (
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES authz.roles (id) ON DELETE CASCADE,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(255),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by  VARCHAR(255),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE authz.role_permissions (
    role_id       UUID NOT NULL REFERENCES authz.roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES authz.permissions (id) ON DELETE CASCADE,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(255),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by    VARCHAR(255),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE authz.user_permissions (
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES authz.permissions (id) ON DELETE CASCADE,
    effect_type   VARCHAR(16)  NOT NULL DEFAULT 'GRANT',
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(255),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by    VARCHAR(255),
    PRIMARY KEY (user_id, permission_id)
);

CREATE TABLE authz.admin_login_logs (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID REFERENCES users (id) ON DELETE SET NULL,
    username     VARCHAR(64) NOT NULL,
    ip_address   VARCHAR(64),
    user_agent   VARCHAR(255),
    logged_in_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_user_permissions_user_id ON authz.user_permissions (user_id);
CREATE INDEX idx_authz_admin_login_logs_user ON authz.admin_login_logs (user_id, logged_in_at DESC);

INSERT INTO authz.roles (code) VALUES ('ADMIN') ON CONFLICT (code) DO NOTHING;
INSERT INTO authz.roles (code) VALUES ('USER') ON CONFLICT (code) DO NOTHING;

INSERT INTO authz.permissions (code, permission_group, action_code) VALUES ('ADMIN_ACCESS', 'ADMIN', 'ACCESS') ON CONFLICT (code) DO NOTHING;
INSERT INTO authz.permissions (code, permission_group, action_code) VALUES ('USER_PROFILE_READ', 'USER_PROFILE', 'READ') ON CONFLICT (code) DO NOTHING;

INSERT INTO users (username, password_hash, enabled, data_scope)
VALUES (
    'admin',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    TRUE,
    'ALL'
) ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password_hash, enabled, data_scope)
VALUES (
    'user',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    TRUE,
    'OWN'
) ON CONFLICT (username) DO NOTHING;

INSERT INTO authz.user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN authz.roles r ON r.code = 'ADMIN' WHERE u.username = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO authz.user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN authz.roles r ON r.code = 'USER' WHERE u.username = 'user'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO authz.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM authz.roles r
JOIN authz.permissions p ON p.code IN ('ADMIN_ACCESS', 'USER_PROFILE_READ')
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO authz.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM authz.roles r
JOIN authz.permissions p ON p.code = 'USER_PROFILE_READ'
WHERE r.code = 'USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;
