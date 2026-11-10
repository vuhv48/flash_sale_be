INSERT INTO authz.roles (code) VALUES ('ORGANIZER') ON CONFLICT (code) DO NOTHING;

INSERT INTO authz.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM authz.roles r
JOIN authz.permissions p ON p.code = 'USER_PROFILE_READ'
WHERE r.code = 'ORGANIZER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

CREATE TABLE organizer_requests (
    id             BIGSERIAL PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users (id),
    org_name       VARCHAR(200) NOT NULL,
    tax_code       VARCHAR(13)  NOT NULL,
    representative VARCHAR(100) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reject_reason  VARCHAR(500),
    reviewed_by    UUID REFERENCES users (id),
    reviewed_at    TIMESTAMPTZ,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(255),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     VARCHAR(255),
    CONSTRAINT ck_organizer_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE UNIQUE INDEX uq_org_request_pending
    ON organizer_requests (user_id)
    WHERE status = 'PENDING' AND is_deleted = FALSE;
