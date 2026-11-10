-- BRD 02: xóa role/permission cũ và tạo lại đúng ma trận phân quyền.
-- Có = ALL, Riêng = OWN, Auto = AUTO. Ô trống trong ma trận không có dòng.

CREATE TEMP TABLE previous_user_role AS
SELECT ur.user_id,
       CASE
           WHEN bool_or(r.code = 'ADMIN') THEN 'ADMIN'
           WHEN bool_or(r.code = 'ORGANIZER') THEN 'ORGANIZER'
           ELSE 'CUSTOMER'
       END AS role_code
FROM authz.user_roles ur
JOIN authz.roles r ON r.id = ur.role_id
WHERE ur.is_deleted = FALSE
GROUP BY ur.user_id;

DELETE FROM authz.user_permissions;
DELETE FROM authz.role_permissions;
DELETE FROM authz.user_roles;
DELETE FROM authz.permissions;
DELETE FROM authz.roles;

ALTER TABLE authz.permissions
    ADD COLUMN name VARCHAR(255) NOT NULL;

ALTER TABLE authz.role_permissions
    ADD COLUMN access_level VARCHAR(16) NOT NULL;

ALTER TABLE authz.role_permissions
    ADD CONSTRAINT ck_role_permissions_access_level
    CHECK (access_level IN ('ALL', 'OWN', 'AUTO'));

INSERT INTO authz.roles (code) VALUES
    ('GUEST'),
    ('CUSTOMER'),
    ('ORGANIZER'),
    ('ADMIN'),
    ('SYSTEM');

INSERT INTO authz.permissions (code, permission_group, action_code, name) VALUES
    ('PER-01', 'ACCOUNT', 'REGISTER_CUSTOMER', 'Đăng ký tài khoản khách hàng'),
    ('PER-02', 'ACCOUNT', 'REGISTER_ORGANIZER', 'Đăng ký tài khoản ban tổ chức'),
    ('PER-03', 'ACCOUNT', 'AUTHENTICATE', 'Đăng nhập, đăng xuất, quên mật khẩu'),
    ('PER-04', 'ACCOUNT', 'MANAGE_PROFILE', 'Xem, sửa hồ sơ, đổi mật khẩu'),
    ('PER-05', 'ACCOUNT', 'READ_USERS', 'Xem danh sách và chi tiết người dùng'),
    ('PER-06', 'ACCOUNT', 'REVIEW_ORGANIZER', 'Duyệt hoặc từ chối tài khoản ban tổ chức'),
    ('PER-07', 'ACCOUNT', 'LOCK_ACCOUNT', 'Khóa, mở khóa tài khoản'),
    ('PER-08', 'ACCOUNT', 'CREATE_ADMIN', 'Tạo tài khoản Admin'),
    ('PER-10', 'EVENT', 'READ_PUBLIC_EVENT', 'Xem sự kiện đã công khai'),
    ('PER-11', 'EVENT', 'CREATE_DRAFT_EVENT', 'Tạo sự kiện nháp, thêm loại vé'),
    ('PER-12', 'EVENT', 'UPDATE_OWN_EVENT', 'Sửa sự kiện ở trạng thái Nháp hoặc Bị từ chối'),
    ('PER-13', 'EVENT', 'SUBMIT_EVENT', 'Gửi sự kiện đi duyệt'),
    ('PER-14', 'EVENT', 'READ_UNPUBLISHED_EVENT', 'Xem sự kiện chưa công khai'),
    ('PER-15', 'EVENT', 'REVIEW_EVENT', 'Duyệt, từ chối sự kiện'),
    ('PER-16', 'EVENT', 'PAUSE_SALE', 'Tạm dừng, tiếp tục đợt bán'),
    ('PER-17', 'EVENT', 'CANCEL_EVENT', 'Hủy sự kiện (kéo theo hoàn tiền hàng loạt)'),
    ('PER-18', 'EVENT', 'ADVANCE_SALE_STATUS', 'Chuyển trạng thái Đang bán / Kết thúc theo giờ'),
    ('PER-20', 'WAITING_ROOM', 'ENTER_WAITING_ROOM', 'Vào phòng chờ, xem vị trí trong hàng'),
    ('PER-21', 'WAITING_ROOM', 'GRANT_ENTRY', 'Cấp lượt vào'),
    ('PER-22', 'WAITING_ROOM', 'HOLD_TICKET', 'Giữ chỗ'),
    ('PER-23', 'WAITING_ROOM', 'CANCEL_HOLD', 'Hủy giữ chỗ'),
    ('PER-24', 'WAITING_ROOM', 'READ_WAITING_ROOM', 'Xem số người trong phòng chờ, số vé đang giữ'),
    ('PER-25', 'WAITING_ROOM', 'ADJUST_ENTRY_RATE', 'Đổi tốc độ cấp lượt khi đang bán'),
    ('PER-30', 'PAYMENT', 'CHECKOUT', 'Tạo đơn, thanh toán'),
    ('PER-31', 'PAYMENT', 'PAYMENT_CALLBACK', 'Xử lý callback thanh toán'),
    ('PER-32', 'PAYMENT', 'READ_ORDER', 'Xem đơn hàng'),
    ('PER-33', 'PAYMENT', 'ISSUE_TICKET', 'Phát vé QR, gửi email'),
    ('PER-34', 'PAYMENT', 'READ_TICKET', 'Xem, tải vé QR'),
    ('PER-35', 'PAYMENT', 'RESEND_TICKET_EMAIL', 'Gửi lại email vé'),
    ('PER-36', 'PAYMENT', 'AUTO_REFUND', 'Hoàn tiền tự động'),
    ('PER-37', 'PAYMENT', 'MANUAL_REFUND', 'Hoàn tiền thủ công'),
    ('PER-38', 'PAYMENT', 'READ_RECONCILIATION', 'Xem kết quả đối soát giao dịch'),
    ('PER-40', 'CHAT', 'OPEN_CONVERSATION', 'Tạo hội thoại mới với Admin'),
    ('PER-41', 'CHAT', 'SEND_MESSAGE', 'Gửi tin nhắn, đính kèm tệp'),
    ('PER-42', 'CHAT', 'READ_CONVERSATION', 'Xem lịch sử hội thoại'),
    ('PER-43', 'CHAT', 'MANAGE_QUEUE', 'Xem hàng đợi hội thoại, nhận xử lý, chuyển cho Admin khác'),
    ('PER-44', 'CHAT', 'CLOSE_CONVERSATION', 'Đóng hội thoại (đánh dấu đã giải quyết)'),
    ('PER-45', 'CHAT', 'REOPEN_CONVERSATION', 'Mở lại hội thoại đã đóng (trong 7 ngày)'),
    ('PER-46', 'CHAT', 'RATE_CONVERSATION', 'Đánh giá hội thoại'),
    ('PER-47', 'CHAT', 'INTERNAL_NOTE', 'Ghi chú nội bộ (khách không thấy)'),
    ('PER-48', 'CHAT', 'MANAGE_CANNED_REPLY', 'Quản lý câu trả lời mẫu'),
    ('PER-49', 'CHAT', 'AUTO_CHAT', 'Tin nhắn tự động ngoài giờ, tự đóng hội thoại im lặng'),
    ('PER-50', 'FILE', 'UPLOAD_AVATAR', 'Upload ảnh đại diện'),
    ('PER-51', 'FILE', 'UPLOAD_EVENT_IMAGE', 'Upload ảnh bìa, ảnh sự kiện'),
    ('PER-52', 'FILE', 'UPLOAD_LEGAL_DOC', 'Upload giấy tờ pháp lý ban tổ chức'),
    ('PER-53', 'FILE', 'UPLOAD_CHAT_FILE', 'Upload tệp đính kèm chat'),
    ('PER-54', 'FILE', 'READ_PUBLIC_FILE', 'Xem tệp công khai (ảnh sự kiện đã công khai)'),
    ('PER-55', 'FILE', 'READ_PRIVATE_FILE', 'Xem tệp riêng tư (giấy tờ, đính kèm chat)'),
    ('PER-56', 'FILE', 'DELETE_FILE', 'Xóa tệp'),
    ('PER-57', 'FILE', 'SCAN_FILE', 'Quét mã độc, dọn tệp mồ côi'),
    ('PER-60', 'REPORT', 'PLATFORM_DASHBOARD', 'Dashboard toàn nền tảng'),
    ('PER-61', 'REPORT', 'EVENT_REPORT', 'Dashboard và báo cáo một sự kiện'),
    ('PER-62', 'REPORT', 'EXPORT_ORDERS', 'Xuất danh sách đơn ra CSV'),
    ('PER-63', 'REPORT', 'READ_AUDIT_LOG', 'Xem nhật ký kiểm toán'),
    ('PER-64', 'REPORT', 'UPDATE_SYSTEM_CONFIG', 'Sửa cấu hình hệ thống (giờ hỗ trợ, giới hạn, hạn mức tệp)'),
    ('PER-65', 'REPORT', 'RECEIVE_OPS_ALERT', 'Nhận cảnh báo vận hành');

INSERT INTO authz.role_permissions (role_id, permission_id, access_level)
SELECT r.id, p.id, v.access_level
FROM (VALUES
    ('GUEST', 'PER-01', 'ALL'),
    ('GUEST', 'PER-02', 'ALL'),
    ('GUEST', 'PER-03', 'ALL'),
    ('CUSTOMER', 'PER-03', 'ALL'),
    ('ORGANIZER', 'PER-03', 'ALL'),
    ('ADMIN', 'PER-03', 'ALL'),
    ('CUSTOMER', 'PER-04', 'OWN'),
    ('ORGANIZER', 'PER-04', 'OWN'),
    ('ADMIN', 'PER-04', 'OWN'),
    ('ADMIN', 'PER-05', 'ALL'),
    ('ADMIN', 'PER-06', 'ALL'),
    ('ADMIN', 'PER-07', 'ALL'),
    ('SYSTEM', 'PER-07', 'AUTO'),
    ('ADMIN', 'PER-08', 'ALL'),
    ('GUEST', 'PER-10', 'ALL'),
    ('CUSTOMER', 'PER-10', 'ALL'),
    ('ORGANIZER', 'PER-10', 'ALL'),
    ('ADMIN', 'PER-10', 'ALL'),
    ('ORGANIZER', 'PER-11', 'ALL'),
    ('ORGANIZER', 'PER-12', 'OWN'),
    ('ORGANIZER', 'PER-13', 'OWN'),
    ('ORGANIZER', 'PER-14', 'OWN'),
    ('ADMIN', 'PER-14', 'ALL'),
    ('ADMIN', 'PER-15', 'ALL'),
    ('ORGANIZER', 'PER-16', 'OWN'),
    ('ADMIN', 'PER-16', 'ALL'),
    ('ADMIN', 'PER-17', 'ALL'),
    ('SYSTEM', 'PER-18', 'AUTO'),
    ('CUSTOMER', 'PER-20', 'ALL'),
    ('SYSTEM', 'PER-21', 'AUTO'),
    ('CUSTOMER', 'PER-22', 'ALL'),
    ('CUSTOMER', 'PER-23', 'OWN'),
    ('SYSTEM', 'PER-23', 'AUTO'),
    ('ORGANIZER', 'PER-24', 'OWN'),
    ('ADMIN', 'PER-24', 'ALL'),
    ('ADMIN', 'PER-25', 'ALL'),
    ('CUSTOMER', 'PER-30', 'OWN'),
    ('SYSTEM', 'PER-31', 'AUTO'),
    ('CUSTOMER', 'PER-32', 'OWN'),
    ('ORGANIZER', 'PER-32', 'OWN'),
    ('ADMIN', 'PER-32', 'ALL'),
    ('SYSTEM', 'PER-33', 'AUTO'),
    ('CUSTOMER', 'PER-34', 'OWN'),
    ('ADMIN', 'PER-34', 'ALL'),
    ('CUSTOMER', 'PER-35', 'OWN'),
    ('ADMIN', 'PER-35', 'ALL'),
    ('SYSTEM', 'PER-36', 'AUTO'),
    ('ADMIN', 'PER-37', 'ALL'),
    ('ADMIN', 'PER-38', 'ALL'),
    ('SYSTEM', 'PER-38', 'AUTO'),
    ('CUSTOMER', 'PER-40', 'ALL'),
    ('ORGANIZER', 'PER-40', 'ALL'),
    ('CUSTOMER', 'PER-41', 'OWN'),
    ('ORGANIZER', 'PER-41', 'OWN'),
    ('ADMIN', 'PER-41', 'ALL'),
    ('CUSTOMER', 'PER-42', 'OWN'),
    ('ORGANIZER', 'PER-42', 'OWN'),
    ('ADMIN', 'PER-42', 'ALL'),
    ('ADMIN', 'PER-43', 'ALL'),
    ('CUSTOMER', 'PER-44', 'OWN'),
    ('ORGANIZER', 'PER-44', 'OWN'),
    ('ADMIN', 'PER-44', 'ALL'),
    ('SYSTEM', 'PER-44', 'AUTO'),
    ('CUSTOMER', 'PER-45', 'OWN'),
    ('ORGANIZER', 'PER-45', 'OWN'),
    ('ADMIN', 'PER-45', 'ALL'),
    ('CUSTOMER', 'PER-46', 'OWN'),
    ('ORGANIZER', 'PER-46', 'OWN'),
    ('ADMIN', 'PER-47', 'ALL'),
    ('ADMIN', 'PER-48', 'ALL'),
    ('SYSTEM', 'PER-49', 'AUTO'),
    ('CUSTOMER', 'PER-50', 'OWN'),
    ('ORGANIZER', 'PER-50', 'OWN'),
    ('ADMIN', 'PER-50', 'OWN'),
    ('ORGANIZER', 'PER-51', 'OWN'),
    ('ORGANIZER', 'PER-52', 'OWN'),
    ('CUSTOMER', 'PER-53', 'OWN'),
    ('ORGANIZER', 'PER-53', 'OWN'),
    ('ADMIN', 'PER-53', 'ALL'),
    ('GUEST', 'PER-54', 'ALL'),
    ('CUSTOMER', 'PER-54', 'ALL'),
    ('ORGANIZER', 'PER-54', 'ALL'),
    ('ADMIN', 'PER-54', 'ALL'),
    ('CUSTOMER', 'PER-55', 'OWN'),
    ('ORGANIZER', 'PER-55', 'OWN'),
    ('ADMIN', 'PER-55', 'ALL'),
    ('CUSTOMER', 'PER-56', 'OWN'),
    ('ORGANIZER', 'PER-56', 'OWN'),
    ('ADMIN', 'PER-56', 'ALL'),
    ('SYSTEM', 'PER-57', 'AUTO'),
    ('ADMIN', 'PER-60', 'ALL'),
    ('ORGANIZER', 'PER-61', 'OWN'),
    ('ADMIN', 'PER-61', 'ALL'),
    ('ORGANIZER', 'PER-62', 'OWN'),
    ('ADMIN', 'PER-62', 'ALL'),
    ('ADMIN', 'PER-63', 'ALL'),
    ('ADMIN', 'PER-64', 'ALL'),
    ('ADMIN', 'PER-65', 'ALL'),
    ('SYSTEM', 'PER-65', 'AUTO')
) AS v(role_code, permission_code, access_level)
JOIN authz.roles r ON r.code = v.role_code
JOIN authz.permissions p ON p.code = v.permission_code;

INSERT INTO authz.user_roles (user_id, role_id)
SELECT b.user_id, r.id
FROM previous_user_role b
JOIN authz.roles r ON r.code = b.role_code;

INSERT INTO authz.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN authz.roles r ON r.code = 'CUSTOMER'
WHERE NOT EXISTS (
    SELECT 1 FROM authz.user_roles ur WHERE ur.user_id = u.id AND ur.is_deleted = FALSE
);

