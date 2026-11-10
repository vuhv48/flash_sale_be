ALTER TABLE users
    ADD COLUMN full_name VARCHAR(255);

ALTER TABLE users
    ADD COLUMN phone VARCHAR(20);

UPDATE authz.permissions
SET name = 'Đăng ký tài khoản (kèm tùy chọn xin làm ban tổ chức)',
    action_code = 'REGISTER'
WHERE code = 'PER-01';

UPDATE authz.permissions
SET name = 'Gửi yêu cầu làm ban tổ chức sau khi đã có tài khoản',
    action_code = 'SUBMIT_ORGANIZER_REQUEST'
WHERE code = 'PER-02';

DELETE FROM authz.role_permissions rp
USING authz.roles r, authz.permissions p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND p.code = 'PER-02';

INSERT INTO authz.role_permissions (role_id, permission_id, access_level)
SELECT r.id, p.id, 'OWN'
FROM authz.roles r
JOIN authz.permissions p ON p.code = 'PER-02'
WHERE r.code = 'CUSTOMER';

UPDATE authz.permissions
SET name = 'Đăng nhập',
    action_code = 'LOGIN'
WHERE code = 'PER-03';

DELETE FROM authz.role_permissions rp
USING authz.roles r, authz.permissions p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND p.code = 'PER-03'
  AND r.code <> 'GUEST';

UPDATE authz.permissions
SET name = 'Xem, sửa hồ sơ; đổi mật khẩu; làm mới token; đăng xuất'
WHERE code = 'PER-04';

UPDATE authz.permissions
SET name = 'Cấp hoặc gỡ vai trò ADMIN, gỡ vai trò ORGANIZER',
    action_code = 'MANAGE_ROLES'
WHERE code = 'PER-08';

INSERT INTO authz.permissions (code, permission_group, action_code, name)
VALUES ('PER-09', 'ACCOUNT', 'VIEW_OWN_ORGANIZER_REQUEST', 'Xem trạng thái yêu cầu làm ban tổ chức của mình');

INSERT INTO authz.role_permissions (role_id, permission_id, access_level)
SELECT r.id, p.id, 'OWN'
FROM authz.roles r
JOIN authz.permissions p ON p.code = 'PER-09'
WHERE r.code IN ('CUSTOMER', 'ORGANIZER');

UPDATE authz.permissions
SET name = 'Upload giấy tờ pháp lý kèm yêu cầu làm ban tổ chức'
WHERE code = 'PER-52';

INSERT INTO authz.role_permissions (role_id, permission_id, access_level)
SELECT r.id, p.id, 'OWN'
FROM authz.roles r
JOIN authz.permissions p ON p.code = 'PER-52'
WHERE r.code = 'CUSTOMER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO authz.user_roles (user_id, role_id)
SELECT ur.user_id, customer.id
FROM authz.user_roles ur
JOIN authz.roles organizer ON organizer.id = ur.role_id AND organizer.code = 'ORGANIZER'
JOIN authz.roles customer ON customer.code = 'CUSTOMER'
WHERE ur.is_deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM authz.user_roles existing
      WHERE existing.user_id = ur.user_id
        AND existing.role_id = customer.id
        AND existing.is_deleted = FALSE
  );
