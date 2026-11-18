-- Ràng buộc CHECK và index có điều kiện cho schema nghiệp vụ.

-- ===== quy trình =====
ALTER TABLE flows ADD CONSTRAINT ck_flows_entity_type CHECK (entity_type IN ('EVENT','ORDER','REFUND','SUPPORT_TICKET'));
-- lần chuyển đầu tiên không có bước trước, các lần sau bắt buộc có
ALTER TABLE event_flows ADD CONSTRAINT ck_event_flows_step CHECK (step_no > 0 AND (step_no = 1) = (from_step_id IS NULL));
ALTER TABLE order_flows ADD CONSTRAINT ck_order_flows_step CHECK (step_no > 0 AND (step_no = 1) = (from_step_id IS NULL));
ALTER TABLE refund_flows ADD CONSTRAINT ck_refund_flows_step CHECK (step_no > 0 AND (step_no = 1) = (from_step_id IS NULL));
ALTER TABLE support_ticket_flows ADD CONSTRAINT ck_support_flows_step CHECK (step_no > 0 AND (step_no = 1) = (from_step_id IS NULL));

-- ===== system_configs =====
-- mỗi nhóm không trùng mã (chỉ tính dòng chưa xóa)
CREATE UNIQUE INDEX uq_system_configs_group_code ON system_configs (group_code, code) WHERE is_deleted = FALSE;
-- value chỉ dùng cho nhóm SETTING, và nhóm SETTING bắt buộc có value
ALTER TABLE system_configs ADD CONSTRAINT ck_system_configs_value CHECK ((group_code = 'SETTING') = (value IS NOT NULL));

-- ===== tệp của sự kiện, giấy tờ yêu cầu vai trò =====
ALTER TABLE event_documents ADD CONSTRAINT ck_event_documents_type CHECK (document_type_code IN ('EVENT_COVER','EVENT_IMAGE'));
-- mỗi sự kiện tối đa 1 ảnh bìa
CREATE UNIQUE INDEX uq_event_documents_one_cover ON event_documents (event_id) WHERE document_type_code = 'EVENT_COVER' AND is_deleted = FALSE;
ALTER TABLE role_request_documents ADD CONSTRAINT ck_role_request_documents_type CHECK (document_type_code IN ('ORG_BUSINESS_LICENSE','ORG_REPRESENTATIVE_ID','ORG_OTHER'));

-- ===== provinces =====
ALTER TABLE provinces ADD CONSTRAINT ck_provinces_type CHECK (type IN ('CITY','PROVINCE'));

-- ===== events =====
ALTER TABLE events ADD CONSTRAINT ck_events_status CHECK (status IN
  ('DRAFT','PENDING_REVIEW','REJECTED','APPROVED','ON_SALE','PAUSED','SALE_ENDED','CANCELLED','COMPLETED'));
ALTER TABLE events ADD CONSTRAINT ck_events_max_tickets CHECK (max_tickets_per_user BETWEEN 1 AND 10);
ALTER TABLE events ADD CONSTRAINT ck_events_time_order CHECK (
      (waiting_room_opens_at IS NULL OR sale_starts_at IS NULL OR waiting_room_opens_at < sale_starts_at)
  AND (sale_starts_at IS NULL OR sale_ends_at IS NULL OR sale_starts_at < sale_ends_at)
  AND (sale_ends_at   IS NULL OR starts_at    IS NULL OR sale_ends_at   <= starts_at)
  AND (starts_at      IS NULL OR ends_at      IS NULL OR starts_at      < ends_at));
ALTER TABLE events ADD CONSTRAINT ck_events_reject_reason CHECK (status <> 'REJECTED'  OR reject_reason IS NOT NULL);
ALTER TABLE events ADD CONSTRAINT ck_events_cancel_reason CHECK (status <> 'CANCELLED' OR cancel_reason IS NOT NULL);

-- ===== ticket_types: chốt chặn cuối cùng chống bán quá số vé =====
ALTER TABLE ticket_types ADD CONSTRAINT ck_ticket_types_price    CHECK (price >= 0 AND price % 1000 = 0);
ALTER TABLE ticket_types ADD CONSTRAINT ck_ticket_types_total    CHECK (total_quantity > 0);
ALTER TABLE ticket_types ADD CONSTRAINT ck_ticket_types_no_oversell CHECK (
  held_quantity >= 0 AND sold_quantity >= 0 AND held_quantity + sold_quantity <= total_quantity);
CREATE UNIQUE INDEX uq_ticket_types_event_name ON ticket_types (event_id, name) WHERE is_deleted = FALSE;

-- ===== reservations =====
ALTER TABLE reservations ADD CONSTRAINT ck_reservations_status   CHECK (status IN ('HELD','CONFIRMED','CANCELLED','EXPIRED'));
ALTER TABLE reservations ADD CONSTRAINT ck_reservations_quantity CHECK (quantity > 0);
-- mỗi người chỉ có 1 giữ chỗ đang hoạt động cho mỗi sự kiện (RSV-BR-02)
CREATE UNIQUE INDEX uq_reservations_active ON reservations (user_id, event_id)
  WHERE status = 'HELD' AND is_deleted = FALSE;

-- ===== orders =====
ALTER TABLE orders ADD CONSTRAINT ck_orders_status CHECK (status IN
  ('PENDING_PAYMENT','PAID','ISSUED','CANCELLED','REFUNDING','REFUNDED'));
ALTER TABLE orders ADD CONSTRAINT ck_orders_amount CHECK (quantity > 0 AND unit_price >= 0 AND total_amount = unit_price * quantity);
-- mỗi giữ chỗ chỉ có 1 đơn còn hiệu lực; đơn hủy/hoàn tiền thì được tạo đơn mới
CREATE UNIQUE INDEX uq_orders_active_per_reservation ON orders (reservation_id)
  WHERE status IN ('PENDING_PAYMENT','PAID','ISSUED') AND is_deleted = FALSE;

-- ===== payment / tickets / refunds =====
ALTER TABLE payment_transactions ADD CONSTRAINT ck_payment_status CHECK (status IN ('INITIATED','SUCCEEDED','FAILED','EXPIRED'));
ALTER TABLE payment_transactions ADD CONSTRAINT ck_payment_amount CHECK (amount >= 0);
ALTER TABLE tickets ADD CONSTRAINT ck_tickets_status CHECK (status IN ('VALID','REVOKED'));
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_status CHECK (status IN ('REQUESTED','PROCESSING','COMPLETED','FAILED'));
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_amount CHECK (amount > 0);

-- ===== chat =====
ALTER TABLE chat_rooms ADD CONSTRAINT ck_chat_rooms_type CHECK (room_type IN ('GROUP','DIRECT','SUPPORT'));
CREATE UNIQUE INDEX uq_chat_rooms_direct_key ON chat_rooms (direct_key) WHERE direct_key IS NOT NULL;
ALTER TABLE chat_room_members ADD CONSTRAINT ck_chat_member_role CHECK (member_role IN ('REQUESTER','AGENT','MEMBER'));
ALTER TABLE chat_messages ADD CONSTRAINT ck_chat_messages_type CHECK (message_type IN ('TEXT','INTERNAL_NOTE','SYSTEM'));
-- tin không phải tin hệ thống thì phải có người gửi
ALTER TABLE chat_messages ADD CONSTRAINT ck_chat_messages_sender CHECK (message_type = 'SYSTEM' OR sender_id IS NOT NULL);
ALTER TABLE chat_support_tickets ADD CONSTRAINT ck_support_status CHECK (status IN ('OPEN','ASSIGNED','WAITING_USER','CLOSED'));
ALTER TABLE chat_support_tickets ADD CONSTRAINT ck_support_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5);
ALTER TABLE chat_support_tickets ADD CONSTRAINT ck_support_topic_ref CHECK (
      (topic <> 'PAYMENT_TICKET' OR order_id IS NOT NULL)
  AND (topic <> 'EVENT'          OR event_id IS NOT NULL));
CREATE UNIQUE INDEX uq_canned_shortcut ON canned_responses (shortcut) WHERE is_deleted = FALSE;

-- ===== tệp =====
ALTER TABLE documents ADD CONSTRAINT ck_documents_size CHECK (size_bytes >= 0);
ALTER TABLE documents ADD CONSTRAINT ck_documents_status CHECK (status IN
  ('PENDING','UPLOADED','READY','ATTACHED','REJECTED','QUARANTINED','DELETED','EXPIRED'));
ALTER TABLE documents ADD CONSTRAINT ck_documents_visibility CHECK (visibility IN ('PUBLIC','PRIVATE'));
ALTER TABLE document_versions ADD CONSTRAINT ck_document_versions_size CHECK (size_bytes >= 0);
ALTER TABLE document_types ADD CONSTRAINT ck_document_types_visibility CHECK (default_visibility IN ('PUBLIC','PRIVATE'));

-- ===== quản trị / kafka =====
ALTER TABLE alerts ADD CONSTRAINT ck_alerts_status   CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED'));
ALTER TABLE integration_outbox ADD CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING','SENT','FAILED'));

-- ====================================================================
-- RÀNG BUỘC CHỐNG LỖI CODE (kiểm tra lại DB ngày 18/11/2026)
-- Mỗi trạng thái bắt buộc có đủ dữ liệu đi kèm; code quên ghi là DB chặn ngay.
-- ====================================================================

-- sự kiện: đã gửi duyệt thì phải đủ thông tin (nháp, bị từ chối, đã hủy được thiếu)
ALTER TABLE events ADD CONSTRAINT ck_events_submitted_complete CHECK (
  status IN ('DRAFT','REJECTED','CANCELLED') OR (
        description IS NOT NULL AND category IS NOT NULL
    AND venue_name IS NOT NULL AND province_id IS NOT NULL
    AND starts_at IS NOT NULL AND ends_at IS NOT NULL
    AND waiting_room_opens_at IS NOT NULL AND sale_starts_at IS NOT NULL AND sale_ends_at IS NOT NULL));
ALTER TABLE events ADD CONSTRAINT ck_events_settings CHECK (
  hold_minutes BETWEEN 1 AND 30 AND admission_rate > 0 AND pause_count BETWEEN 0 AND 3);
ALTER TABLE events ADD CONSTRAINT ck_events_paused_at CHECK (status <> 'PAUSED' OR paused_at IS NOT NULL);

-- giữ chỗ
ALTER TABLE reservations ADD CONSTRAINT ck_reservations_release_reason CHECK (release_reason IS NULL OR release_reason IN
  ('USER_CANCELLED','EXPIRED','ACCOUNT_LOCKED','EVENT_CANCELLED'));
ALTER TABLE reservations ADD CONSTRAINT ck_reservations_state_data CHECK (
      (status <> 'CONFIRMED' OR confirmed_at IS NOT NULL)
  AND (status NOT IN ('CANCELLED','EXPIRED') OR (released_at IS NOT NULL AND release_reason IS NOT NULL)));

-- đơn hàng
ALTER TABLE orders ADD CONSTRAINT ck_orders_state_data CHECK (
      (status NOT IN ('PAID','ISSUED','REFUNDING','REFUNDED') OR paid_at IS NOT NULL)
  AND (status <> 'ISSUED' OR issued_at IS NOT NULL)
  AND (status <> 'CANCELLED' OR cancelled_at IS NOT NULL));

-- thanh toán: thành công thì phải có số tiền cổng báo về và thời điểm
ALTER TABLE payment_transactions ADD CONSTRAINT ck_payment_succeeded_data CHECK (
  status <> 'SUCCEEDED' OR (paid_amount IS NOT NULL AND completed_at IS NOT NULL));

-- vé, hoàn tiền
ALTER TABLE tickets ADD CONSTRAINT ck_tickets_revoked_data CHECK (status <> 'REVOKED' OR revoked_at IS NOT NULL);
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_state_data CHECK (
      (status <> 'COMPLETED' OR completed_at IS NOT NULL)
  AND retry_count BETWEEN 0 AND 5);

-- dữ liệu tiền không bao giờ xóa, kể cả xóa mềm
ALTER TABLE orders               ADD CONSTRAINT ck_orders_never_deleted   CHECK (is_deleted = FALSE);
ALTER TABLE payment_transactions ADD CONSTRAINT ck_payment_never_deleted  CHECK (is_deleted = FALSE);
ALTER TABLE tickets              ADD CONSTRAINT ck_tickets_never_deleted  CHECK (is_deleted = FALSE);
ALTER TABLE refunds              ADD CONSTRAINT ck_refunds_never_deleted  CHECK (is_deleted = FALSE);

-- tệp
ALTER TABLE documents ADD CONSTRAINT ck_documents_state_data CHECK (
      (status <> 'ATTACHED' OR attached_at IS NOT NULL)
  AND (status NOT IN ('REJECTED','QUARANTINED') OR reject_reason IS NOT NULL));

-- email
ALTER TABLE mail_queue ADD CONSTRAINT ck_mail_queue_status CHECK (status IN ('PENDING','SENT','FAILED'));
ALTER TABLE mail_queue ADD CONSTRAINT ck_mail_queue_sent_data CHECK (status <> 'SENT' OR sent_at IS NOT NULL);

-- chat hỗ trợ: đang xử lý thì phải có Admin phụ trách; đã đóng thì phải có thời điểm đóng
ALTER TABLE chat_support_tickets ADD CONSTRAINT ck_support_state_data CHECK (
      (status NOT IN ('ASSIGNED','WAITING_USER') OR assigned_admin_id IS NOT NULL)
  AND (status <> 'CLOSED' OR closed_at IS NOT NULL));

-- ====================================================================
-- INDEX CÓ ĐIỀU KIỆN CHO JOB QUÉT (theo cách sale-app làm với integration_outbox, mail_queue)
-- Chỉ chứa các dòng job cần đọc, nên nhỏ và nhanh hơn index thường.
-- ====================================================================
CREATE INDEX idx_reservations_held_expires ON reservations (expires_at) WHERE status = 'HELD';
CREATE INDEX idx_payment_initiated ON payment_transactions (created_at) WHERE status = 'INITIATED';
CREATE INDEX idx_refunds_retry ON refunds (next_retry_at) WHERE status IN ('REQUESTED','FAILED');
CREATE INDEX idx_documents_pending_ready ON documents (created_at) WHERE status IN ('PENDING','READY');
CREATE INDEX idx_integration_outbox_pending ON integration_outbox (created_at) WHERE status = 'PENDING';
CREATE INDEX idx_mail_queue_pending ON mail_queue (created_at) WHERE status = 'PENDING';
