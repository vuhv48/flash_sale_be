-- Quyền gán thẳng cho user "user" (không đi qua role).
INSERT INTO authz.user_permissions (user_id, permission_id, effect_type)
SELECT u.id, p.id, 'GRANT'
FROM users u
JOIN authz.permissions p ON p.code = 'USER_PROFILE_READ'
WHERE u.username = 'user'
ON CONFLICT (user_id, permission_id) DO NOTHING;

-- Một dòng nhật ký đăng nhập mẫu cho admin.
INSERT INTO authz.admin_login_logs (user_id, username, ip_address, user_agent, logged_in_at)
SELECT u.id, u.username, '127.0.0.1', 'seed', now()
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM authz.admin_login_logs l
      WHERE l.username = 'admin'
        AND l.user_agent = 'seed'
  );

-- Refresh token thô dùng để test: dev-test-refresh-token-001
-- token_hash = SHA-256 hex của chuỗi UTF-8 đó.
INSERT INTO refresh_tokens (user_id, token_hash, expires_at, revoked)
SELECT u.id,
       '7d534154c286239185abf6b56a469d2b9152a5de4f0557dc9c92d59a69d886af',
       now() + interval '7 days',
       FALSE
FROM users u
WHERE u.username = 'admin'
ON CONFLICT (token_hash) DO NOTHING;
