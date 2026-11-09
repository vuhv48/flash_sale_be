# Chức năng đăng ký, đăng nhập, làm mới token và đổi mật khẩu

Ứng dụng Spring Boot, package `io.github.vuhv48.flashsale`. Phần đang chạy là **đăng nhập / đăng ký / phiên làm việc**. Chưa có API sản phẩm, giỏ hàng, đơn flash sale.

Các API công khai (không cần token): `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/refresh`, `GET /actuator/health`. Mọi request khác phải gửi `Authorization: Bearer {{accessToken}}`.

Các lệnh dùng biến Postman `base_url` = `http://localhost:8088`. Trong Postman chọn **Import → Raw text**, dán cả khối `curl`, rồi bấm Import. Không dán vào ô URL.

## API

### Đăng ký

`POST /api/auth/register` — không cần token. Trả `201`.

```json
{
  "username": "vuhoang",
  "password": "password123",
  "email": "vu@example.com"
}
```

- `username`: bắt buộc, 3–64 ký tự.
- `password`: bắt buộc, 8–128 ký tự.
- `email`: không bắt buộc, tối đa 320 ký tự. Nếu có thì phải đúng định dạng `ten@tenmien`, ví dụ `vu@example.com`. Sai định dạng: `400 VALIDATION_ERROR`, message `email: Email không đúng định dạng`. Email được lưu vào bảng `users` và không được trùng. Trùng email: `409 EMAIL_TAKEN`.

```bash
curl --location '{{base_url}}/api/auth/register' \
--header 'Content-Type: application/json' \
--data '{"username":"vuhoang","password":"password123","email":"vu@example.com"}'
```

Hệ thống tạo user bật sẵn, `dataScope = OWN`, gán role `USER`, rồi cấp access token và refresh token. Gọi lại đúng username đó thì `409 USERNAME_TAKEN`. Dùng lại đúng lệnh trên.

### Đăng nhập

`POST /api/auth/login` — không cần token. Trả `200`.

```json
{
  "username": "admin",
  "password": "password"
}
```

```bash
curl --location '{{base_url}}/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{"username":"admin","password":"password"}'
```

Sai mật khẩu: `401 AUTH_FAILED`. Tài khoản bị khóa: `403 ACCOUNT_DISABLED`.

```bash
curl --location '{{base_url}}/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{"username":"admin","password":"sai-mat-khau"}'
```

Mỗi IP được tối đa **5 lần trong 1 phút** (đếm theo `X-Forwarded-For`, rồi `X-Real-IP`, rồi địa chỉ kết nối). Vượt ngưỡng: `429 TOO_MANY_REQUESTS`. Đăng nhập thành công thì xóa bộ đếm của IP đó. Redis không chạy thì giới hạn được bỏ qua và cho đăng nhập.

Mỗi lần đăng nhập thành công ghi một dòng vào `authz.admin_login_logs` (user, username, IP, User-Agent, thời điểm).

### Làm mới token

`POST /api/auth/refresh` — không cần token. Trả `200`.

```json
{
  "refreshToken": "<refresh token đang còn hiệu lực>"
}
```

```bash
curl --location '{{base_url}}/api/auth/refresh' \
--header 'Content-Type: application/json' \
--data '{"refreshToken":"<refreshToken vừa nhận từ login hoặc register>"}'
```

Token seed của `admin` (migration `V2`, còn hạn 7 ngày kể từ lúc migration chạy):

```bash
curl --location '{{base_url}}/api/auth/refresh' \
--header 'Content-Type: application/json' \
--data '{"refreshToken":"dev-test-refresh-token-001"}'
```

Token cũ bị thu hồi, hệ thống cấp cặp token mới. Token sai, đã thu hồi, hoặc hết hạn: `401 REFRESH_TOKEN_INVALID`. Tài khoản bị khóa: `403 ACCOUNT_DISABLED`.

### Đổi mật khẩu

`POST /api/auth/change-password` — bắt buộc access token. Thành công trả `200`:

```json
{
  "message": "Đổi mật khẩu thành công"
}
```

```json
{
  "currentPassword": "password123",
  "newPassword": "password456"
}
```

```bash
curl --location '{{base_url}}/api/auth/change-password' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"currentPassword":"password123","newPassword":"password456"}'
```

Thiếu hoặc sai access token thì `401 UNAUTHORIZED`:

```bash
curl --location '{{base_url}}/api/auth/change-password' \
--header 'Content-Type: application/json' \
--data '{"currentPassword":"password123","newPassword":"password456"}'
```

`newPassword` dài 8–128 ký tự. Mật khẩu hiện tại sai: `400 PASSWORD_MISMATCH`. Đổi xong, mọi refresh token đang hoạt động của user bị đánh dấu `revoked = true`. Dòng không bị xóa khỏi bảng `refresh_tokens`.

### Đăng xuất

`POST /api/auth/logout` — bắt buộc access token, không có body. Thành công trả `200`:

```json
{
  "message": "Đăng xuất thành công"
}
```

```bash
curl --location --request POST '{{base_url}}/api/auth/logout' \
--header 'Authorization: Bearer {{accessToken}}'
```

Mọi refresh token đang hoạt động của user bị đánh dấu `revoked = true`. Gọi `/api/auth/refresh` bằng token cũ sẽ nhận `401 REFRESH_TOKEN_INVALID`. Access token đã cấp vẫn dùng được đến khi hết hạn. Thiếu hoặc sai access token: `401`.

### Phản hồi token

Đăng ký, đăng nhập và refresh trả cùng một dạng:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 86400,
  "refreshExpiresInSeconds": 604800
}
```

Access token là JWT, hạn mặc định **1 ngày**. Refresh token là chuỗi ngẫu nhiên, hạn mặc định **7 ngày**, chỉ lưu bản băm SHA-256 trong bảng `refresh_tokens`.

JWT chứa: `sub` = username, `iss` = `flash-sale`, `uid`, `authorities` (role và permission, nối bằng dấu phẩy).

Đăng nhập lại sẽ thu hồi mọi refresh token cũ của user đó rồi cấp cặp mới.

### Health

`GET /actuator/health` — không cần token.

```bash
curl --location '{{base_url}}/actuator/health'
```

### Lỗi

Lỗi được trả về dạng:

```json
{
  "timestamp": "...",
  "status": 401,
  "code": "AUTH_FAILED",
  "message": "...",
  "path": "/api/auth/login",
  "details": {}
}
```

| Mã | HTTP | Khi nào |
| --- | --- | --- |
| `AUTH_FAILED` | 401 | Sai mật khẩu |
| `UNAUTHORIZED` | 401 | Chưa đăng nhập hoặc token không hợp lệ |
| `REFRESH_TOKEN_INVALID` | 401 | Refresh token sai hoặc hết hạn |
| `FORBIDDEN` | 403 | Không đủ quyền |
| `ACCOUNT_DISABLED` | 403 | Tài khoản bị khóa |
| `TOO_MANY_REQUESTS` | 429 | Đăng nhập quá 5 lần/phút/IP |
| `VALIDATION_ERROR` | 400 | Body không đúng ràng buộc |
| `PASSWORD_MISMATCH` | 400 | Mật khẩu hiện tại không khớp |
| `USER_NOT_FOUND` | 404 | Không tìm thấy user |
| `USERNAME_TAKEN` | 409 | Username đã có |
| `EMAIL_TAKEN` | 409 | Email đã có |
| `ILLEGAL_STATE` | 409 | Thiếu role `USER` trong database |
| `DATA_INTEGRITY` | 409 | Trùng khóa hoặc vi phạm ràng buộc |
| `INTERNAL_ERROR` | 500 | Lỗi hệ thống |

## Có trong code, chưa có API

Hai hàm này nằm trong `AuthAccountService`, chưa có endpoint:

- `searchUsersByName(name)` — tìm tối đa 20 user chưa xóa, username chứa chuỗi tìm kiếm, không phân biệt hoa thường. Kết quả: `id`, `username`, `enabled`, `dataScope`.
- `setUserEnabledByUsername(username, enabled)` — khóa hoặc mở tài khoản. Khi khóa, thu hồi refresh token của user đó.

## Mail sau đăng ký

Sau khi đăng ký, hệ thống gửi mail chào mừng cho user và mail báo user mới cho admin.

- Không cấu hình SMTP thì bỏ qua, đăng ký vẫn thành công.
- Không có email hợp lệ thì không gửi mail chào mừng.
- Mail admin chỉ gửi khi có `app.mail.admin-notification-address`.
- Gửi mail lỗi thì ghi log, không làm fail đăng ký.

## Dữ liệu

Flyway tạo schema khi ứng dụng khởi động (`ddl-auto: validate`).

| Bảng | Nội dung |
| --- | --- |
| `users` | Tài khoản: username, email (unique, được phép trống), mật khẩu BCrypt, enabled, data scope |
| `authz.roles` | Role |
| `authz.permissions` | Quyền |
| `authz.user_roles` | User thuộc role nào |
| `authz.role_permissions` | Role có quyền nào |
| `authz.user_permissions` | Quyền gán thẳng cho user (`GRANT` / `DENY`) |
| `authz.admin_login_logs` | Lịch sử đăng nhập |
| `refresh_tokens` | Bản băm refresh token, hạn, đã thu hồi hay chưa |

Dữ liệu sẵn sau migration:

| Username | Mật khẩu | Role | Data scope | Quyền |
| --- | --- | --- | --- | --- |
| `admin` | `password` | `ADMIN` | `ALL` | `ADMIN_ACCESS`, `USER_PROFILE_READ` |
| `user` | `password` | `USER` | `OWN` | `USER_PROFILE_READ` |

User tự đăng ký nhận role `USER` và data scope `OWN`.

`V2` còn thêm: quyền `USER_PROFILE_READ` gán thẳng cho user `user`, một dòng log đăng nhập mẫu của `admin`, và một refresh token dev cho `admin` (raw token `dev-test-refresh-token-001`, hạn 7 ngày kể từ lúc migration chạy).

## Cấu hình mặc định

| Mục | Giá trị |
| --- | --- |
| Cổng | `8088` |
| Database | `jdbc:postgresql://localhost:5432/flash_sale`, user `postgres` |
| Redis | `localhost:6379` |
| Access token | 86400 giây |
| Refresh token | 604800 giây |
| CORS | `http://localhost:3000`, `http://localhost:3002` |
| Docker Compose | tắt (`spring.docker.compose.enabled=false`) |

Biến môi trường ghi đè được: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `JWT_SECRET`, `MAIL_FROM`, `ADMIN_NOTIFICATION_EMAIL`, `APP_SECURITY_CORS_ALLOWED_ORIGINS`.
