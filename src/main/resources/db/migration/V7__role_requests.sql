ALTER TABLE authz.roles
    ADD COLUMN name VARCHAR(100),
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN requestable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN requires_org_profile BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE authz.roles
SET name = 'Khách vãng lai', description = 'Mặc định khi chưa đăng nhập', requestable = FALSE, requires_org_profile = FALSE
WHERE code = 'GUEST';

UPDATE authz.roles
SET name = 'Khách hàng', description = 'Tự có khi đăng ký', requestable = TRUE, requires_org_profile = FALSE
WHERE code = 'CUSTOMER';

UPDATE authz.roles
SET name = 'Ban tổ chức', description = 'Xin thêm trong hồ sơ, kèm hồ sơ đơn vị, Admin duyệt', requestable = TRUE, requires_org_profile = TRUE
WHERE code = 'ORGANIZER';

UPDATE authz.roles
SET name = 'Quản trị viên', description = 'Không xin được. Admin cấp thẳng', requestable = FALSE, requires_org_profile = FALSE
WHERE code = 'ADMIN';

UPDATE authz.roles
SET name = 'Hệ thống', description = 'Tác vụ nền, không đăng nhập', requestable = FALSE, requires_org_profile = FALSE
WHERE code = 'SYSTEM';

ALTER TABLE authz.roles
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE authz.roles
    ADD CONSTRAINT ck_admin_not_requestable CHECK (NOT (code = 'ADMIN' AND requestable));

CREATE TABLE role_requests (
    id             BIGSERIAL PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users (id),
    role_code      VARCHAR(64)  NOT NULL REFERENCES authz.roles (code),
    action         VARCHAR(10)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    user_note      VARCHAR(500),
    reject_reason  VARCHAR(500),
    reviewed_by    UUID REFERENCES users (id),
    reviewed_at    TIMESTAMPTZ,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(255),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     VARCHAR(255),
    CONSTRAINT ck_role_request_action CHECK (action IN ('ADD', 'REMOVE')),
    CONSTRAINT ck_role_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE UNIQUE INDEX uq_role_request_pending
    ON role_requests (user_id, role_code)
    WHERE status = 'PENDING' AND is_deleted = FALSE;

CREATE TABLE organizer_profiles (
    user_id        UUID PRIMARY KEY REFERENCES users (id),
    org_name       VARCHAR(200) NOT NULL,
    tax_code       VARCHAR(13)  NOT NULL,
    representative VARCHAR(100) NOT NULL,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(255),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     VARCHAR(255)
);

CREATE UNIQUE INDEX uq_org_tax_code
    ON organizer_profiles (tax_code)
    WHERE is_deleted = FALSE;

CREATE TABLE authz.role_change_logs (
    id             BIGSERIAL PRIMARY KEY,
    actor_user_id  UUID REFERENCES users (id),
    target_user_id UUID         NOT NULL REFERENCES users (id),
    role_code      VARCHAR(64)  NOT NULL,
    action         VARCHAR(10)  NOT NULL,
    source         VARCHAR(20)  NOT NULL,
    request_id     BIGINT REFERENCES role_requests (id),
    reason         VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_role_change_action CHECK (action IN ('ADD', 'REMOVE')),
    CONSTRAINT ck_role_change_source CHECK (source IN ('REQUEST', 'DIRECT'))
);

INSERT INTO organizer_profiles (user_id, org_name, tax_code, representative, created_at, created_by)
SELECT DISTINCT ON (tax_code) user_id, org_name, tax_code, representative, created_at, created_by
FROM organizer_requests
WHERE is_deleted = FALSE
ORDER BY tax_code, created_at DESC;

INSERT INTO role_requests (
    user_id, role_code, action, status, reject_reason, reviewed_by, reviewed_at,
    is_deleted, created_at, created_by, updated_at, updated_by
)
SELECT user_id, 'ORGANIZER', 'ADD', status, reject_reason, reviewed_by, reviewed_at,
       is_deleted, created_at, created_by, updated_at, updated_by
FROM organizer_requests
WHERE is_deleted = FALSE;

DROP TABLE organizer_requests;

UPDATE authz.permissions
SET name = 'Đăng ký tài khoản (tự có vai trò CUSTOMER)'
WHERE code = 'PER-01';

UPDATE authz.permissions
SET name = 'Gửi yêu cầu thêm hoặc gỡ vai trò (trừ ADMIN); hủy yêu cầu đang chờ'
WHERE code = 'PER-02';

INSERT INTO authz.role_permissions (role_id, permission_id, access_level)
SELECT r.id, p.id, 'OWN'
FROM authz.roles r
JOIN authz.permissions p ON p.code = 'PER-02'
WHERE r.code = 'ORGANIZER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

UPDATE authz.permissions
SET name = 'Xem danh sách yêu cầu thêm/gỡ vai trò; duyệt hoặc từ chối'
WHERE code = 'PER-06';

UPDATE authz.permissions
SET name = 'Cấp hoặc gỡ thẳng bất kỳ vai trò nào (kể cả ADMIN), không qua yêu cầu'
WHERE code = 'PER-08';

UPDATE authz.permissions
SET name = 'Xem vai trò hiện có và lịch sử yêu cầu của mình'
WHERE code = 'PER-09';

INSERT INTO authz.role_permissions (role_id, permission_id, access_level)
SELECT r.id, p.id, 'OWN'
FROM authz.roles r
JOIN authz.permissions p ON p.code = 'PER-09'
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO authz.permissions (code, permission_group, action_code, name)
VALUES ('PER-09a', 'ACCOUNT', 'MANAGE_ROLE_CATALOG', 'Quản lý danh mục vai trò (thêm vai trò, bật/tắt cờ được xin)');

INSERT INTO authz.role_permissions (role_id, permission_id, access_level)
SELECT r.id, p.id, 'ALL'
FROM authz.roles r
JOIN authz.permissions p ON p.code = 'PER-09a'
WHERE r.code = 'ADMIN';
