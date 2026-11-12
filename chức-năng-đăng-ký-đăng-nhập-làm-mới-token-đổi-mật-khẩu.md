# Chức năng đăng ký, đăng nhập, làm mới token và đổi mật khẩu

Ứng dụng Spring Boot, package `io.github.vuhv48.flashsale`. Phần đang chạy là **đăng nhập / đăng ký / phiên làm việc**. Chưa có API sản phẩm, giỏ hàng, đơn flash sale.

Các API công khai (không cần token): `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/refresh`, `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`, `GET /actuator/health`, cùng trang Swagger. Mọi API còn lại phải gửi `Authorization: Bearer {{accessToken}}`.

Swagger UI: [http://localhost:8088/swagger-ui.html](http://localhost:8088/swagger-ui.html). Mô tả OpenAPI nằm ở `/v3/api-docs`. Bấm **Authorize**, dán access token (không cần chữ `Bearer `) rồi thử các API cần đăng nhập.

Các lệnh dùng biến Postman `base_url` = `http://localhost:8088`. Trong Postman chọn **Import → Raw text**, dán cả khối `curl`, rồi bấm Import. Không dán vào ô URL.

## API

### Đăng ký

`POST /api/auth/register` — không cần token. Trả `201`.

```json
{
  "email": "vu@example.com",
  "password": "MatKhau123",
  "confirmPassword": "MatKhau123",
  "fullName": "Vũ Hoàng",
  "phone": "0912345678",
  "acceptedTerms": true
}
```

- `email`: bắt buộc, tối đa 254 ký tự, đúng định dạng email. Hệ thống lưu chữ thường. Username của tài khoản mới chính là email này. Trùng email, không phân biệt hoa thường, với tài khoản chưa xóa: `409 EMAIL_TAKEN`, message `Email đã được sử dụng.`
- `password` và `confirmPassword`: bắt buộc, 8–64 ký tự, có chữ hoa, chữ thường và số, không được trùng email. Hai ô phải khớp. Thiếu một trong các điều kiện: `400 VALIDATION_ERROR`.
- `fullName`: bắt buộc, 2–100 ký tự.
- `phone`: bắt buộc, đúng `0` rồi 9 chữ số, ví dụ `0912345678`. Trùng số với tài khoản chưa xóa: `409 PHONE_TAKEN`.
- `acceptedTerms`: phải là `true`.
- Form không có chọn vai trò.

```bash
curl --location '{{base_url}}/api/auth/register' \
--header 'Content-Type: application/json' \
--data '{"email":"vu@example.com","password":"MatKhau123","confirmPassword":"MatKhau123","fullName":"Vũ Hoàng","phone":"0912345678","acceptedTerms":true}'
```

Hệ thống tạo user bật sẵn, `dataScope = OWN`, gán role `CUSTOMER`, rồi cấp access token và refresh token. Message là `Đăng ký thành công.` Mỗi IP tối đa **5 lần đăng ký trong 1 giờ**. Vượt ngưỡng: `429 TOO_MANY_REQUESTS`. Redis không chạy thì giới hạn được bỏ qua.

### Đăng nhập

`POST /api/auth/login` — không cần token. Trả `200`.

```json
{
  "email": "admin@example.com",
  "password": "password"
}
```

```bash
curl --location '{{base_url}}/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{"email":"admin@example.com","password":"password"}'
```

Sai email hoặc sai mật khẩu: `401 AUTH_FAILED`, message `Email hoặc mật khẩu không đúng.` Tài khoản bị Admin khóa: `403 ACCOUNT_DISABLED`, message `Tài khoản đã bị khóa. Liên hệ hỗ trợ qua mục Chat để biết thêm.`

```bash
curl --location '{{base_url}}/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{"email":"admin@example.com","password":"sai-mat-khau"}'
```

Mỗi IP được tối đa **20 lần trong 1 phút** (đếm theo `X-Forwarded-For`, rồi `X-Real-IP`, rồi địa chỉ kết nối). Vượt ngưỡng: `429 TOO_MANY_REQUESTS`. Bộ đếm IP không xóa khi đăng nhập đúng. Redis không chạy thì giới hạn được bỏ qua.

Sai mật khẩu **5 lần liên tiếp** trên cùng một email thì lần thứ 6 bị khóa 15 phút, kể cả khi mật khẩu lần đó đúng: `429 LOGIN_LOCKED`, message `Bạn đã nhập sai quá 5 lần. Vui lòng thử lại sau 15 phút.` Lần thứ 5 vẫn trả `AUTH_FAILED`. Đăng nhập đúng trước khi đủ 5 lần thì xóa bộ đếm sai của email đó.

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
  "currentPassword": "MatKhau123",
  "newPassword": "MatKhauMoi1"
}
```

```bash
curl --location '{{base_url}}/api/auth/change-password' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"currentPassword":"MatKhau123","newPassword":"MatKhauMoi1"}'
```

Thiếu hoặc sai access token thì `401 UNAUTHORIZED`:

```bash
curl --location '{{base_url}}/api/auth/change-password' \
--header 'Content-Type: application/json' \
--data '{"currentPassword":"MatKhau123","newPassword":"MatKhauMoi1"}'
```

`newPassword` dài 8–64 ký tự, có chữ hoa, chữ thường và số, không trùng email. Mật khẩu hiện tại sai: `400 PASSWORD_MISMATCH`. Đổi xong, mọi refresh token đang hoạt động của user bị đánh dấu `revoked = true`. Dòng không bị xóa khỏi bảng `refresh_tokens`.

### Quên mật khẩu

`POST /api/auth/forgot-password` — không cần token. Luôn trả `200` cùng một câu, dù email có hay không:

```json
{
  "message": "Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi."
}
```

```bash
curl --location '{{base_url}}/api/auth/forgot-password' \
--header 'Content-Type: application/json' \
--data '{"email":"vu@example.com"}'
```

Email tồn tại và tài khoản đang bật thì hệ thống tạo mã hiệu lực **30 phút**, lưu bản băm SHA-256, và gửi mã tới email. Chưa cấu hình SMTP thì mã được ghi vào log, không trả trong HTTP.

`POST /api/auth/reset-password` — không cần token. Thành công trả `200`:

```json
{
  "message": "Đặt lại mật khẩu thành công."
}
```

```bash
curl --location '{{base_url}}/api/auth/reset-password' \
--header 'Content-Type: application/json' \
--data '{"token":"<mã trong mail hoặc log>","newPassword":"MatKhauMoi1"}'
```

Mã sai, đã dùng, hoặc hết hạn: `400 RESET_TOKEN_INVALID`. Mật khẩu mới theo cùng quy tắc đăng ký. Đặt lại xong, mọi refresh token đang hoạt động bị thu hồi.

### Hồ sơ

`GET /api/me/profile` và `PUT /api/me/profile` — bắt buộc access token và quyền `PER-04`. Email không đổi.

```bash
curl --location '{{base_url}}/api/me/profile' \
--header 'Authorization: Bearer {{accessToken}}'
```

```bash
curl --location --request PUT '{{base_url}}/api/me/profile' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"fullName":"Vũ Hoàng","phone":"0987654321"}'
```

```json
{
  "email": "vu@example.com",
  "fullName": "Vũ Hoàng",
  "phone": "0987654321",
  "enabled": true
}
```

Số điện thoại trùng tài khoản khác chưa xóa: `409 PHONE_TAKEN`.

### Khóa tài khoản

Admin có `PER-05` xem tối đa 20 tài khoản chưa xóa. Bỏ trống `name` thì lấy 20 tài khoản đầu. Có `name` thì lọc theo email, username hoặc họ tên.

```bash
curl --location '{{base_url}}/api/admin/users?name=vu' \
--header 'Authorization: Bearer {{accessToken}}'
```

Admin có `PER-07` khóa hoặc mở khóa. Username của tài khoản đăng ký mới trùng email. Bắt buộc có lý do. Admin không tự khóa mình: `409 CANNOT_LOCK_SELF`.

```bash
curl --location '{{base_url}}/api/admin/users/vu@example.com/lock' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"reason":"Vi phạm điều khoản sử dụng nhiều lần"}'
```

```bash
curl --location '{{base_url}}/api/admin/users/vu@example.com/unlock' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"reason":"Đã xử lý xong"}'
```

Khóa thì `enabled = false`, thu hồi refresh token, ghi `authz.account_lock_logs`. Request tiếp theo của tài khoản đó nhận `401`. Mở khóa thì `enabled = true`.

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

### Xin thêm hoặc gỡ vai trò

Đăng ký không chọn vai trò. Người đã đăng nhập tự xin thêm hoặc gỡ mọi vai trò có `requestable = true`. Xem vai trò và lịch sử cần `PER-09`. Gửi hoặc hủy yêu cầu cần `PER-02`. Admin duyệt cần `PER-06`. Cấp hoặc gỡ thẳng cần `PER-08`. `ADMIN` không có `PER-02`. `ADMIN` bị khóa trong database, không xin được. Vai trò chỉ đổi khi Admin duyệt. Tài khoản luôn còn ít nhất một vai trò.

Vai trò được phép xin:

```bash
curl --location '{{base_url}}/api/roles/requestable' \
--header 'Authorization: Bearer {{accessToken}}'
```

Vai trò hiện có:

```bash
curl --location '{{base_url}}/api/me/roles' \
--header 'Authorization: Bearer {{accessToken}}'
```

Quyền hiệu lực của chính user đang đăng nhập, dùng để ẩn hiện chức năng trên giao diện. Quyền lấy từ mọi role đang có, cộng quyền gán thẳng, trừ quyền bị cấm thẳng. Cùng một mã quyền thì giữ mức rộng hơn: `ALL` rộng hơn `OWN`, `OWN` rộng hơn `AUTO`.

```bash
curl --location '{{base_url}}/api/me/permissions' \
--header 'Authorization: Bearer {{accessToken}}'
```

```json
[
  {
    "code": "PER-04",
    "name": "Xem, sửa hồ sơ; đổi mật khẩu; làm mới token; đăng xuất",
    "group": "ACCOUNT",
    "action": "MANAGE_PROFILE",
    "accessLevel": "OWN"
  }
]
```

Gửi yêu cầu. `organizer` chỉ bắt buộc khi xin thêm vai trò có `requires_org_profile = true`, hiện là `ORGANIZER`.

```bash
curl --location '{{base_url}}/api/role-requests' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"roleCode":"ORGANIZER","action":"ADD","note":"Công ty tổ chức concert","organizer":{"orgName":"Công ty Sự kiện ABC","taxCode":"0101234567","representative":"Vũ Hoàng"}}'
```

Xin role không được phép: `409 ROLE_NOT_REQUESTABLE`. Đã có yêu cầu chờ cho đúng vai trò đó: `409 ROLE_REQUEST_PENDING`. Xin thêm vai trò đã có: `409 ROLE_ALREADY_ASSIGNED`. Xin gỡ vai trò chưa có: `409 ROLE_NOT_ASSIGNED`. Gỡ vai trò cuối: `409 LAST_ROLE`.

Lịch sử yêu cầu của mình:

```bash
curl --location '{{base_url}}/api/role-requests/me' \
--header 'Authorization: Bearer {{accessToken}}'
```

Hủy khi còn `PENDING`:

```bash
curl --location --request POST '{{base_url}}/api/role-requests/1/cancel' \
--header 'Authorization: Bearer {{accessToken}}'
```

Admin xem danh sách chờ. Có thể lọc thêm `roleCode` và `action`.

```bash
curl --location '{{base_url}}/api/admin/role-requests?status=PENDING' \
--header 'Authorization: Bearer {{accessToken}}'
```

Duyệt thì thêm hoặc gỡ đúng vai trò trong yêu cầu, cùng một transaction. Gỡ `ORGANIZER` khi còn sự kiện chưa kết thúc thì bị chặn.

```bash
curl --location --request POST '{{base_url}}/api/admin/role-requests/1/approve' \
--header 'Authorization: Bearer {{accessToken}}'
```

Từ chối, lý do từ 10 đến 500 ký tự:

```bash
curl --location --request POST '{{base_url}}/api/admin/role-requests/1/reject' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"reason":"Thiếu giấy tờ đăng ký kinh doanh"}'
```

Admin cấp thẳng, kể cả `ADMIN`:

```bash
curl --location '{{base_url}}/api/admin/users/vuhoang/roles' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"roleCode":"ADMIN","reason":"Cấp quyền quản trị"}'
```

Admin gỡ thẳng. Không tự gỡ `ADMIN` của mình và không gỡ admin cuối cùng.

```bash
curl --location --request DELETE '{{base_url}}/api/admin/users/vuhoang/roles/ORGANIZER' \
--header 'Authorization: Bearer {{accessToken}}'
```

Gỡ vai trò thì thu hồi refresh token ngay. Thêm vai trò thì request tiếp theo đã đọc role mới từ database.

### Phản hồi token

Đăng ký, đăng nhập và refresh trả cùng một dạng:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshExpiresInSeconds": 604800
}
```

Access token là JWT, hạn mặc định **15 phút** (900 giây). Refresh token là chuỗi ngẫu nhiên, hạn mặc định **7 ngày**, chỉ lưu bản băm SHA-256 trong bảng `refresh_tokens`.

JWT chứa: `sub` = username, `iss` = `flash-sale`, `uid`, `authorities` (role và permission, nối bằng dấu phẩy). Tài khoản đăng ký mới có username trùng email. Filter đọc `sub`: nếu có `@` thì tìm theo email, không thì tìm theo username.

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
| `AUTH_FAILED` | 401 | Email hoặc mật khẩu không đúng |
| `UNAUTHORIZED` | 401 | Chưa đăng nhập, token không hợp lệ, hoặc tài khoản đã bị khóa sau khi cấp token |
| `REFRESH_TOKEN_INVALID` | 401 | Refresh token sai hoặc hết hạn |
| `LOGIN_LOCKED` | 429 | Sai mật khẩu 5 lần, khóa đăng nhập 15 phút |
| `RESET_TOKEN_INVALID` | 400 | Mã đặt lại mật khẩu sai, đã dùng, hoặc hết hạn |
| `FORBIDDEN` | 403 | Không đủ quyền, ví dụ thiếu `PER-02`, `PER-04`, `PER-05`, `PER-06`, `PER-07`, `PER-08`, `PER-09` |
| `ACCOUNT_DISABLED` | 403 | Đăng nhập khi tài khoản bị khóa |
| `TOO_MANY_REQUESTS` | 429 | Đăng nhập quá 20 lần/phút/IP, hoặc đăng ký quá 5 lần/giờ/IP |
| `VALIDATION_ERROR` | 400 | Body không đúng ràng buộc |
| `PASSWORD_MISMATCH` | 400 | Mật khẩu hiện tại không khớp |
| `USER_NOT_FOUND` | 404 | Không tìm thấy user |
| `EMAIL_TAKEN` | 409 | Email đã có ở tài khoản chưa xóa |
| `PHONE_TAKEN` | 409 | Số điện thoại đã có ở tài khoản chưa xóa |
| `CANNOT_LOCK_SELF` | 409 | Admin tự khóa tài khoản của mình |
| `ROLE_NOT_REQUESTABLE` | 409 | Vai trò không được phép xin, kể cả `ADMIN` |
| `ROLE_REQUEST_PENDING` | 409 | Đã có yêu cầu chờ cho đúng vai trò này |
| `ROLE_REQUEST_NOT_PENDING` | 409 | Yêu cầu không còn chờ duyệt |
| `ROLE_ALREADY_ASSIGNED` | 409 | Xin thêm vai trò đang có |
| `ROLE_NOT_ASSIGNED` | 409 | Xin gỡ vai trò chưa có |
| `LAST_ROLE` | 409 | Gỡ vai trò cuối cùng |
| `LAST_ADMIN` | 409 | Gỡ quản trị viên cuối cùng |
| `CANNOT_REMOVE_OWN_ADMIN` | 409 | Admin tự gỡ `ADMIN` của mình |
| `ORGANIZER_HAS_OPEN_EVENTS` | 409 | Gỡ `ORGANIZER` khi còn sự kiện chưa kết thúc |
| `ROLE_REQUEST_NOT_FOUND` | 404 | Không tìm thấy yêu cầu |
| `ROLE_NOT_FOUND` | 404 | Không tìm thấy vai trò |
| `TAX_CODE_TAKEN` | 409 | Mã số thuế đã có |
| `ILLEGAL_STATE` | 409 | Thiếu role cần dùng trong database, hoặc không cấp được quyền |
| `DATA_INTEGRITY` | 409 | Trùng khóa hoặc vi phạm ràng buộc |
| `INTERNAL_ERROR` | 500 | Lỗi hệ thống |

## Mail sau đăng ký

Sau khi đăng ký, hệ thống gửi mail chào mừng cho user và mail báo user mới cho admin.

- Không cấu hình SMTP thì bỏ qua, đăng ký vẫn thành công.
- Chưa cấu hình SMTP thì mã quên mật khẩu được ghi vào log, không trả trong HTTP.
- Mail admin chỉ gửi khi có `app.mail.admin-notification-address`.
- Gửi mail lỗi thì ghi log, không làm fail đăng ký.

## Dữ liệu

Flyway tạo schema khi ứng dụng khởi động (`ddl-auto: validate`).

| Bảng | Nội dung |
| --- | --- |
| `users` | Tài khoản: username, email, họ tên, số điện thoại, mật khẩu BCrypt, enabled, data scope. Email và số điện thoại unique theo bản ghi chưa xóa |
| `authz.roles` | Danh mục vai trò: mã, tên, `requestable`, `requires_org_profile`. `ADMIN` không được bật `requestable` |
| `authz.permissions` | Quyền |
| `authz.user_roles` | User có những role nào. Một tài khoản có thể có nhiều role, ví dụ `CUSTOMER` và `ORGANIZER` |
| `authz.role_permissions` | Role có quyền nào |
| `authz.user_permissions` | Quyền gán thẳng cho user (`GRANT` / `DENY`) |
| `authz.admin_login_logs` | Lịch sử đăng nhập |
| `refresh_tokens` | Bản băm refresh token, hạn, đã thu hồi hay chưa |
| `password_reset_tokens` | Bản băm mã đặt lại mật khẩu, hạn 30 phút, đã dùng hay chưa |
| `authz.account_lock_logs` | Nhật ký khóa và mở khóa: tài khoản, người thực hiện, lý do |
| `role_requests` | Yêu cầu thêm hoặc gỡ mọi vai trò: `ADD` / `REMOVE`, trạng thái `PENDING` / `APPROVED` / `REJECTED` / `CANCELLED`. Mỗi user một yêu cầu `PENDING` cho mỗi vai trò |
| `organizer_profiles` | Hồ sơ đơn vị, một dòng cho một user: tên đơn vị, mã số thuế, người đại diện |
| `authz.role_change_logs` | Nhật ký mỗi lần thêm hoặc gỡ vai trò, qua yêu cầu hoặc Admin cấp thẳng |

Dữ liệu sẵn sau migration:

| Username | Email | Mật khẩu | Role | Data scope |
| --- | --- | --- | --- | --- | --- |
| `admin` | `admin@example.com` | `password` | `ADMIN` | `ALL` |
| `user` | `user@example.com` | `password` | `CUSTOMER` | `OWN` |

User tự đăng ký nhận role `CUSTOMER` và data scope `OWN`. Muốn thêm hoặc gỡ vai trò khác thì gửi yêu cầu, Admin duyệt. `ADMIN` chỉ được Admin cấp thẳng.

Role trong hệ thống: `GUEST`, `CUSTOMER`, `ORGANIZER`, `ADMIN`, `SYSTEM`. Quyền là `PER-01` đến `PER-65`. `Có` lưu `ALL`, `Riêng` lưu `OWN`, `Auto` lưu `AUTO`. Ô không được phép không có dòng trong `authz.role_permissions`.

`V2` còn một dòng log đăng nhập mẫu của `admin` và một refresh token dev cho `admin` (raw token `dev-test-refresh-token-001`, hạn 7 ngày kể từ lúc migration chạy). `V5` đã xóa quyền gán thẳng `USER_PROFILE_READ`.

## Cấu hình mặc định

| Mục | Giá trị |
| --- | --- |
| Cổng | `8088` |
| Database | `jdbc:postgresql://localhost:5432/flash_sale`, user `postgres` |
| Redis | `localhost:6379` |
| Access token | 900 giây |
| Refresh token | 604800 giây |
| CORS | `http://localhost:3000`, `http://localhost:3002` |
| Docker Compose | tắt (`spring.docker.compose.enabled=false`) |

Biến môi trường ghi đè được: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `JWT_SECRET`, `MAIL_FROM`, `ADMIN_NOTIFICATION_EMAIL`, `APP_SECURITY_CORS_ALLOWED_ORIGINS`.
