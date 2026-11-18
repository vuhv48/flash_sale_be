ALTER TABLE users ALTER COLUMN username TYPE VARCHAR(254);
ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(254);
ALTER TABLE users ALTER COLUMN full_name TYPE VARCHAR(100);

ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_email;

CREATE UNIQUE INDEX uq_users_email_active
    ON users (lower(email))
    WHERE is_deleted = FALSE AND email IS NOT NULL;

CREATE UNIQUE INDEX uq_users_phone_active
    ON users (phone)
    WHERE is_deleted = FALSE AND phone IS NOT NULL;

UPDATE users SET email = 'admin@example.com' WHERE username = 'admin' AND email IS NULL;
UPDATE users SET email = 'user@example.com' WHERE username = 'user' AND email IS NULL;

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE authz.account_lock_logs (
    id             BIGSERIAL PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users (id),
    actor_user_id  UUID REFERENCES users (id),
    action         VARCHAR(10)  NOT NULL,
    reason         VARCHAR(500) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_account_lock_action CHECK (action IN ('LOCK', 'UNLOCK'))
);
