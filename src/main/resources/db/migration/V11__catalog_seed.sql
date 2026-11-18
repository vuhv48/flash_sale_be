-- Dữ liệu danh mục: provinces, system_configs, flows, flow_steps, mail_templates, document_types.

-- ====================================================================
-- DỮ LIỆU DANH MỤC: chạy ngay sau khi tạo bảng, code cần có sẵn
-- Thứ tự: provinces -> system_configs -> flows -> flow_steps -> mail_templates -> document_types
-- (flow_steps tra flow_id theo mã nên phải chạy sau flows)
-- ====================================================================

-- ====================================================================
-- DỮ LIỆU DANH MỤC: 34 tỉnh, thành phố theo Quyết định 19/2025/QĐ-TTg (hiệu lực 01/07/2025)
-- ====================================================================
INSERT INTO provinces (gso_code, name, short_name, type, order_no, created_by, updated_by) VALUES
('01', 'Thành phố Hà Nội', 'Hà Nội', 'CITY', 1, 'system', 'system'),
('04', 'Tỉnh Cao Bằng', 'Cao Bằng', 'PROVINCE', 2, 'system', 'system'),
('08', 'Tỉnh Tuyên Quang', 'Tuyên Quang', 'PROVINCE', 3, 'system', 'system'),
('11', 'Tỉnh Điện Biên', 'Điện Biên', 'PROVINCE', 4, 'system', 'system'),
('12', 'Tỉnh Lai Châu', 'Lai Châu', 'PROVINCE', 5, 'system', 'system'),
('14', 'Tỉnh Sơn La', 'Sơn La', 'PROVINCE', 6, 'system', 'system'),
('15', 'Tỉnh Lào Cai', 'Lào Cai', 'PROVINCE', 7, 'system', 'system'),
('19', 'Tỉnh Thái Nguyên', 'Thái Nguyên', 'PROVINCE', 8, 'system', 'system'),
('20', 'Tỉnh Lạng Sơn', 'Lạng Sơn', 'PROVINCE', 9, 'system', 'system'),
('22', 'Tỉnh Quảng Ninh', 'Quảng Ninh', 'PROVINCE', 10, 'system', 'system'),
('24', 'Tỉnh Bắc Ninh', 'Bắc Ninh', 'PROVINCE', 11, 'system', 'system'),
('25', 'Tỉnh Phú Thọ', 'Phú Thọ', 'PROVINCE', 12, 'system', 'system'),
('31', 'Thành phố Hải Phòng', 'Hải Phòng', 'CITY', 13, 'system', 'system'),
('33', 'Tỉnh Hưng Yên', 'Hưng Yên', 'PROVINCE', 14, 'system', 'system'),
('37', 'Tỉnh Ninh Bình', 'Ninh Bình', 'PROVINCE', 15, 'system', 'system'),
('38', 'Tỉnh Thanh Hóa', 'Thanh Hóa', 'PROVINCE', 16, 'system', 'system'),
('40', 'Tỉnh Nghệ An', 'Nghệ An', 'PROVINCE', 17, 'system', 'system'),
('42', 'Tỉnh Hà Tĩnh', 'Hà Tĩnh', 'PROVINCE', 18, 'system', 'system'),
('44', 'Tỉnh Quảng Trị', 'Quảng Trị', 'PROVINCE', 19, 'system', 'system'),
('46', 'Thành phố Huế', 'Huế', 'CITY', 20, 'system', 'system'),
('48', 'Thành phố Đà Nẵng', 'Đà Nẵng', 'CITY', 21, 'system', 'system'),
('51', 'Tỉnh Quảng Ngãi', 'Quảng Ngãi', 'PROVINCE', 22, 'system', 'system'),
('52', 'Tỉnh Gia Lai', 'Gia Lai', 'PROVINCE', 23, 'system', 'system'),
('56', 'Tỉnh Khánh Hòa', 'Khánh Hòa', 'PROVINCE', 24, 'system', 'system'),
('66', 'Tỉnh Đắk Lắk', 'Đắk Lắk', 'PROVINCE', 25, 'system', 'system'),
('68', 'Tỉnh Lâm Đồng', 'Lâm Đồng', 'PROVINCE', 26, 'system', 'system'),
('75', 'Tỉnh Đồng Nai', 'Đồng Nai', 'PROVINCE', 27, 'system', 'system'),
('79', 'Thành phố Hồ Chí Minh', 'Hồ Chí Minh', 'CITY', 28, 'system', 'system'),
('80', 'Tỉnh Tây Ninh', 'Tây Ninh', 'PROVINCE', 29, 'system', 'system'),
('82', 'Tỉnh Đồng Tháp', 'Đồng Tháp', 'PROVINCE', 30, 'system', 'system'),
('86', 'Tỉnh Vĩnh Long', 'Vĩnh Long', 'PROVINCE', 31, 'system', 'system'),
('91', 'Tỉnh An Giang', 'An Giang', 'PROVINCE', 32, 'system', 'system'),
('92', 'Thành phố Cần Thơ', 'Cần Thơ', 'CITY', 33, 'system', 'system'),
('96', 'Tỉnh Cà Mau', 'Cà Mau', 'PROVINCE', 34, 'system', 'system');

-- ====================================================================
-- DỮ LIỆU DANH MỤC: system_configs
-- ====================================================================
INSERT INTO system_configs (group_code, code, label, value, order_no, created_by, updated_by) VALUES
('GENDER', 'MALE', 'Nam', NULL, 1, 'system', 'system'),
('GENDER', 'FEMALE', 'Nữ', NULL, 2, 'system', 'system'),
('GENDER', 'OTHER', 'Khác', NULL, 3, 'system', 'system'),
('EVENT_CATEGORY', 'MUSIC', 'Âm nhạc', NULL, 1, 'system', 'system'),
('EVENT_CATEGORY', 'SPORT', 'Thể thao', NULL, 2, 'system', 'system'),
('EVENT_CATEGORY', 'CONFERENCE', 'Hội thảo', NULL, 3, 'system', 'system'),
('EVENT_CATEGORY', 'THEATER', 'Sân khấu', NULL, 4, 'system', 'system'),
('EVENT_CATEGORY', 'OTHER', 'Khác', NULL, 5, 'system', 'system'),
('CHAT_TOPIC', 'PAYMENT_TICKET', 'Thanh toán và vé', NULL, 1, 'system', 'system'),
('CHAT_TOPIC', 'ACCOUNT', 'Tài khoản', NULL, 2, 'system', 'system'),
('CHAT_TOPIC', 'EVENT', 'Sự kiện', NULL, 3, 'system', 'system'),
('CHAT_TOPIC', 'FEEDBACK', 'Góp ý', NULL, 4, 'system', 'system'),
('CHAT_TOPIC', 'OTHER', 'Khác', NULL, 5, 'system', 'system'),
('REFUND_REASON', 'LATE_PAYMENT', 'Thanh toán đến muộn, đã hết vé', NULL, 1, 'system', 'system'),
('REFUND_REASON', 'EVENT_CANCELLED', 'Sự kiện bị hủy', NULL, 2, 'system', 'system'),
('REFUND_REASON', 'AMOUNT_MISMATCH', 'Số tiền thanh toán không khớp', NULL, 3, 'system', 'system'),
('REFUND_REASON', 'MANUAL', 'Admin hoàn tiền thủ công', NULL, 4, 'system', 'system'),
('ALERT_SEVERITY', 'CRITICAL', 'Nghiêm trọng', NULL, 1, 'system', 'system'),
('ALERT_SEVERITY', 'HIGH', 'Cao', NULL, 2, 'system', 'system'),
('ALERT_SEVERITY', 'MEDIUM', 'Trung bình', NULL, 3, 'system', 'system'),
('ALERT_SEVERITY', 'LOW', 'Thấp', NULL, 4, 'system', 'system'),
('SETTING', 'HOLD_MINUTES', 'Thời gian giữ chỗ mặc định (phút)', '10', 1, 'system', 'system'),
('SETTING', 'ADMIT_TTL_MINUTES', 'Hiệu lực lượt vào từ phòng chờ (phút)', '5', 2, 'system', 'system'),
('SETTING', 'DEFAULT_ADMISSION_RATE', 'Tốc độ cấp lượt mặc định (người/giây)', '200', 3, 'system', 'system'),
('SETTING', 'SUPPORT_WORKING_HOURS', 'Giờ làm việc của bộ phận hỗ trợ (giờ Việt Nam)', '08:00-22:00', 4, 'system', 'system'),
('SETTING', 'CHAT_FIRST_RESPONSE_SLA_MINUTES', 'SLA phản hồi chat đầu tiên (phút)', '5', 5, 'system', 'system'),
('SETTING', 'FILE_USER_QUOTA_MB', 'Hạn mức lưu trữ tệp mỗi người (MB)', '100', 6, 'system', 'system');

-- ====================================================================
-- DỮ LIỆU DANH MỤC: quy trình và các bước
-- ====================================================================
INSERT INTO flows (code, name, entity_type, created_by, updated_by) VALUES
('EVENT_LIFECYCLE', 'Quy trình sự kiện', 'EVENT', 'system', 'system'),
('ORDER_LIFECYCLE', 'Quy trình đơn hàng', 'ORDER', 'system', 'system'),
('REFUND_PROCESS', 'Quy trình hoàn tiền', 'REFUND', 'system', 'system'),
('SUPPORT_TICKET', 'Quy trình hỗ trợ', 'SUPPORT_TICKET', 'system', 'system');

INSERT INTO flow_steps (flow_id, code, name, step_order, is_initial, is_final, created_by, updated_by)
SELECT f.id, v.code, v.name, v.step_order, v.is_initial, v.is_final, 'system', 'system'
FROM (VALUES
  ('EVENT_LIFECYCLE', 'DRAFT', 'Nháp', 1, TRUE, FALSE),
  ('EVENT_LIFECYCLE', 'PENDING_REVIEW', 'Chờ duyệt', 2, FALSE, FALSE),
  ('EVENT_LIFECYCLE', 'REJECTED', 'Bị từ chối', 3, FALSE, FALSE),
  ('EVENT_LIFECYCLE', 'APPROVED', 'Đã duyệt, sắp mở bán', 4, FALSE, FALSE),
  ('EVENT_LIFECYCLE', 'ON_SALE', 'Đang bán', 5, FALSE, FALSE),
  ('EVENT_LIFECYCLE', 'PAUSED', 'Tạm dừng', 6, FALSE, FALSE),
  ('EVENT_LIFECYCLE', 'SALE_ENDED', 'Kết thúc bán', 7, FALSE, FALSE),
  ('EVENT_LIFECYCLE', 'CANCELLED', 'Đã hủy', 8, FALSE, TRUE),
  ('EVENT_LIFECYCLE', 'COMPLETED', 'Đã diễn ra', 9, FALSE, TRUE),
  ('ORDER_LIFECYCLE', 'PENDING_PAYMENT', 'Chờ thanh toán', 1, TRUE, FALSE),
  ('ORDER_LIFECYCLE', 'PAID', 'Đã thanh toán', 2, FALSE, FALSE),
  ('ORDER_LIFECYCLE', 'ISSUED', 'Đã phát vé', 3, FALSE, TRUE),
  ('ORDER_LIFECYCLE', 'CANCELLED', 'Đã hủy', 4, FALSE, TRUE),
  ('ORDER_LIFECYCLE', 'REFUNDING', 'Chờ hoàn tiền', 5, FALSE, FALSE),
  ('ORDER_LIFECYCLE', 'REFUNDED', 'Đã hoàn tiền', 6, FALSE, TRUE),
  ('REFUND_PROCESS', 'REQUESTED', 'Đã yêu cầu', 1, TRUE, FALSE),
  ('REFUND_PROCESS', 'PROCESSING', 'Đang xử lý', 2, FALSE, FALSE),
  ('REFUND_PROCESS', 'FAILED', 'Thất bại, chờ thử lại', 3, FALSE, FALSE),
  ('REFUND_PROCESS', 'COMPLETED', 'Đã hoàn tiền', 4, FALSE, TRUE),
  ('SUPPORT_TICKET', 'OPEN', 'Mới', 1, TRUE, FALSE),
  ('SUPPORT_TICKET', 'ASSIGNED', 'Đang xử lý', 2, FALSE, FALSE),
  ('SUPPORT_TICKET', 'WAITING_USER', 'Chờ người yêu cầu', 3, FALSE, FALSE),
  ('SUPPORT_TICKET', 'CLOSED', 'Đã đóng', 4, FALSE, TRUE)
) AS v(flow_code, code, name, step_order, is_initial, is_final)
JOIN flows f ON f.code = v.flow_code;

-- ====================================================================
-- DỮ LIỆU DANH MỤC: mẫu email
-- ====================================================================
INSERT INTO mail_templates (code, subject_template, body_html, created_by, updated_by) VALUES
('ORDER_TICKETS', 'Vé của bạn - Đơn {{orderCode}}', '<p>Xin chào <strong>{{fullName}}</strong>,</p><p>Đơn <strong>{{orderCode}}</strong> đã thanh toán thành công. Vé QR của bạn ở trong mục Vé của tôi.</p>', 'system', 'system'),
('REFUND_COMPLETED', 'Đơn {{orderCode}} đã được hoàn tiền', '<p>Đơn <strong>{{orderCode}}</strong> đã được hoàn {{amount}} đ. Thời gian tiền về tùy ngân hàng.</p>', 'system', 'system'),
('EVENT_CANCELLED', 'Sự kiện {{eventName}} bị hủy', '<p>Sự kiện <strong>{{eventName}}</strong> đã bị hủy. Đơn {{orderCode}} sẽ được hoàn tiền tự động.</p>', 'system', 'system'),
('EVENT_REVIEWED', 'Sự kiện {{eventName}}: {{result}}', '<p>Sự kiện <strong>{{eventName}}</strong> {{result}}.</p><p>{{reason}}</p>', 'system', 'system'),
('CHAT_NEW_MESSAGE', 'Bạn có tin nhắn mới từ bộ phận hỗ trợ', '<p>Bạn có tin nhắn mới trong cuộc trò chuyện <strong>{{roomName}}</strong>.</p>', 'system', 'system'),
('ROLE_REQUEST_REVIEWED', 'Yêu cầu vai trò {{roleName}}: {{result}}', '<p>Yêu cầu {{action}} vai trò <strong>{{roleName}}</strong> {{result}}.</p><p>{{reason}}</p>', 'system', 'system');

-- ===== Loại tệp và quy tắc từng loại =====
INSERT INTO document_types (code, name, require_verification, max_size_mb, allowed_mime_pattern, max_files_per_ref, default_visibility, created_by, updated_by) VALUES
('AVATAR',          'Ảnh đại diện',                TRUE,   2, '^image/(jpeg|png|webp)$',                 1,    'PUBLIC',  'system', 'system'),
('EVENT_COVER',     'Ảnh bìa sự kiện',             TRUE,   5, '^image/(jpeg|png|webp)$',                 1,    'PUBLIC',  'system', 'system'),
('EVENT_IMAGE',     'Ảnh phụ sự kiện',             TRUE,   5, '^image/(jpeg|png|webp)$',                 10,   'PUBLIC',  'system', 'system'),
('ORG_BUSINESS_LICENSE',  'Giấy phép kinh doanh',      TRUE,  10, '^(image/(jpeg|png)|application/pdf)$', 2, 'PRIVATE', 'system', 'system'),
('ORG_REPRESENTATIVE_ID', 'Giấy tờ người đại diện',    TRUE,  10, '^(image/(jpeg|png)|application/pdf)$', 2, 'PRIVATE', 'system', 'system'),
('ORG_OTHER',             'Giấy tờ khác',              TRUE,  10, '^(image/(jpeg|png)|application/pdf)$', 5, 'PRIVATE', 'system', 'system'),
('CHAT_ATTACHMENT', 'Tệp đính kèm chat',           TRUE,   5, '^(image/(jpeg|png|webp)|application/pdf)$', 5,  'PRIVATE', 'system', 'system'),
('REPORT_EXPORT',   'Tệp xuất báo cáo',            FALSE, 50, '^text/csv$',                              NULL, 'PRIVATE', 'system', 'system');
