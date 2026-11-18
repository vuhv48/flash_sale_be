-- Business schema: events, orders, payments, tickets, files, chat, audit, mail.
-- Depends on public.users and public.role_requests from V1-V8.
-- SQL dump generated using DBML (dbml.dbdiagram.io)
-- Database: PostgreSQL
-- Generated at: 2026-11-18T07:00:55.675Z

CREATE TABLE "system_configs" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "group_code" varchar(50) NOT NULL,
  "code" varchar(50) NOT NULL,
  "label" varchar(200) NOT NULL,
  "value" varchar(500),
  "description" varchar(500),
  "order_no" int NOT NULL DEFAULT 0,
  "is_active" boolean NOT NULL DEFAULT true,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "provinces" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "gso_code" varchar(2) UNIQUE NOT NULL,
  "name" varchar(100) NOT NULL,
  "short_name" varchar(100) NOT NULL,
  "type" varchar(20) NOT NULL,
  "order_no" int NOT NULL DEFAULT 0,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "events" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "code" varchar(20) UNIQUE NOT NULL,
  "organizer_id" uuid NOT NULL,
  "name" varchar(200) NOT NULL,
  "description" text,
  "category" varchar(50),
  "venue_name" varchar(200),
  "venue_address" varchar(300),
  "province_id" uuid,
  "starts_at" timestamptz,
  "ends_at" timestamptz,
  "waiting_room_opens_at" timestamptz,
  "sale_starts_at" timestamptz,
  "sale_ends_at" timestamptz,
  "max_tickets_per_user" int NOT NULL DEFAULT 4,
  "hold_minutes" int NOT NULL DEFAULT 10,
  "admission_rate" int NOT NULL DEFAULT 200,
  "status" varchar(20) NOT NULL DEFAULT 'DRAFT',
  "flow_id" uuid NOT NULL,
  "current_step_id" uuid NOT NULL,
  "submitted_at" timestamptz,
  "reviewed_by" uuid,
  "reviewed_at" timestamptz,
  "reject_reason" varchar(500),
  "pause_count" int NOT NULL DEFAULT 0,
  "paused_at" timestamptz,
  "cancelled_by" uuid,
  "cancelled_at" timestamptz,
  "cancel_reason" varchar(500),
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "event_documents" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "event_id" uuid NOT NULL,
  "document_type_code" varchar(100) NOT NULL,
  "document_id" uuid NOT NULL,
  "sort_order" int NOT NULL DEFAULT 0,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "ticket_types" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "event_id" uuid NOT NULL,
  "name" varchar(50) NOT NULL,
  "price" bigint NOT NULL,
  "total_quantity" int NOT NULL,
  "held_quantity" int NOT NULL DEFAULT 0,
  "sold_quantity" int NOT NULL DEFAULT 0,
  "sale_paused" boolean NOT NULL DEFAULT false,
  "sort_order" int NOT NULL DEFAULT 0,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "reservations" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "user_id" uuid NOT NULL,
  "event_id" uuid NOT NULL,
  "ticket_type_id" uuid NOT NULL,
  "quantity" int NOT NULL,
  "status" varchar(20) NOT NULL DEFAULT 'HELD',
  "expires_at" timestamptz NOT NULL,
  "extended" boolean NOT NULL DEFAULT false,
  "confirmed_at" timestamptz,
  "released_at" timestamptz,
  "release_reason" varchar(30),
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "orders" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "order_code" varchar(20) NOT NULL,
  "user_id" uuid NOT NULL,
  "event_id" uuid NOT NULL,
  "reservation_id" uuid NOT NULL,
  "ticket_type_id" uuid NOT NULL,
  "quantity" int NOT NULL,
  "unit_price" bigint NOT NULL,
  "total_amount" bigint NOT NULL,
  "status" varchar(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
  "flow_id" uuid NOT NULL,
  "current_step_id" uuid NOT NULL,
  "idempotency_key" varchar(100) NOT NULL,
  "paid_at" timestamptz,
  "issued_at" timestamptz,
  "cancelled_at" timestamptz,
  "cancel_reason" varchar(300),
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "payment_transactions" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "order_id" uuid NOT NULL,
  "gateway" varchar(20) NOT NULL DEFAULT 'MOCK',
  "gateway_txn_id" varchar(100) NOT NULL,
  "amount" bigint NOT NULL,
  "paid_amount" bigint,
  "status" varchar(20) NOT NULL DEFAULT 'INITIATED',
  "payment_url" varchar(500),
  "completed_at" timestamptz,
  "last_queried_at" timestamptz,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "payment_callback_logs" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "gateway" varchar(20) NOT NULL,
  "gateway_txn_id" varchar(100),
  "payload" jsonb NOT NULL,
  "signature_valid" boolean NOT NULL,
  "result" varchar(30) NOT NULL,
  "received_at" timestamptz NOT NULL DEFAULT (now())
);

CREATE TABLE "tickets" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "order_id" uuid NOT NULL,
  "event_id" uuid NOT NULL,
  "ticket_type_id" uuid NOT NULL,
  "user_id" uuid NOT NULL,
  "ticket_code" varchar(40) NOT NULL,
  "qr_signature" varchar(128) NOT NULL,
  "status" varchar(20) NOT NULL DEFAULT 'VALID',
  "issued_at" timestamptz NOT NULL DEFAULT (now()),
  "revoked_at" timestamptz,
  "revoke_reason" varchar(300),
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "refunds" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "order_id" uuid NOT NULL,
  "amount" bigint NOT NULL,
  "reason_type" varchar(50) NOT NULL,
  "reason" varchar(500),
  "status" varchar(20) NOT NULL DEFAULT 'REQUESTED',
  "flow_id" uuid NOT NULL,
  "current_step_id" uuid NOT NULL,
  "gateway_refund_id" varchar(100),
  "retry_count" int NOT NULL DEFAULT 0,
  "next_retry_at" timestamptz,
  "last_error" varchar(500),
  "requested_by" uuid,
  "completed_at" timestamptz,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "flows" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "code" varchar(50) UNIQUE NOT NULL,
  "name" varchar(200) NOT NULL,
  "entity_type" varchar(30) NOT NULL,
  "description" varchar(500),
  "enabled" boolean NOT NULL DEFAULT true,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "flow_steps" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "flow_id" uuid NOT NULL,
  "code" varchar(30) NOT NULL,
  "name" varchar(200) NOT NULL,
  "step_order" int NOT NULL,
  "is_initial" boolean NOT NULL DEFAULT false,
  "is_final" boolean NOT NULL DEFAULT false,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "event_flows" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "event_id" uuid NOT NULL,
  "step_no" int NOT NULL,
  "from_step_id" uuid,
  "flow_step_id" uuid NOT NULL,
  "action" varchar(50) NOT NULL,
  "processed_by" uuid,
  "processed_by_name" varchar(255) NOT NULL,
  "reason" varchar(500),
  "metadata" jsonb,
  "trace_id" varchar(64),
  "processed_at" timestamptz NOT NULL DEFAULT (now())
);

CREATE TABLE "order_flows" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "order_id" uuid NOT NULL,
  "step_no" int NOT NULL,
  "from_step_id" uuid,
  "flow_step_id" uuid NOT NULL,
  "action" varchar(50) NOT NULL,
  "processed_by" uuid,
  "processed_by_name" varchar(255) NOT NULL,
  "reason" varchar(500),
  "metadata" jsonb,
  "trace_id" varchar(64),
  "processed_at" timestamptz NOT NULL DEFAULT (now())
);

CREATE TABLE "refund_flows" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "refund_id" uuid NOT NULL,
  "step_no" int NOT NULL,
  "from_step_id" uuid,
  "flow_step_id" uuid NOT NULL,
  "action" varchar(50) NOT NULL,
  "processed_by" uuid,
  "processed_by_name" varchar(255) NOT NULL,
  "reason" varchar(500),
  "metadata" jsonb,
  "trace_id" varchar(64),
  "processed_at" timestamptz NOT NULL DEFAULT (now())
);

CREATE TABLE "support_ticket_flows" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "room_id" uuid NOT NULL,
  "step_no" int NOT NULL,
  "from_step_id" uuid,
  "flow_step_id" uuid NOT NULL,
  "action" varchar(50) NOT NULL,
  "processed_by" uuid,
  "processed_by_name" varchar(255) NOT NULL,
  "reason" varchar(500),
  "metadata" jsonb,
  "trace_id" varchar(64),
  "processed_at" timestamptz NOT NULL DEFAULT (now())
);

CREATE TABLE "document_types" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "code" varchar(100) UNIQUE NOT NULL,
  "name" varchar(255) NOT NULL,
  "active" boolean NOT NULL DEFAULT true,
  "require_verification" boolean NOT NULL DEFAULT true,
  "max_size_mb" int,
  "allowed_mime_pattern" varchar(255),
  "max_files_per_ref" int,
  "default_visibility" varchar(10) NOT NULL DEFAULT 'PRIVATE',
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "documents" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "provider" varchar(20) NOT NULL DEFAULT 'minio',
  "bucket" varchar(255) NOT NULL,
  "file_path" varchar(1024) NOT NULL,
  "original_filename" varchar(512),
  "mime_type" varchar(255),
  "size_bytes" bigint NOT NULL,
  "status" varchar(30) NOT NULL DEFAULT 'PENDING',
  "document_type_code" varchar(100) NOT NULL,
  "current_version_id" uuid,
  "owner_id" uuid NOT NULL,
  "visibility" varchar(10) NOT NULL DEFAULT 'PRIVATE',
  "reject_reason" varchar(300),
  "variants" jsonb,
  "upload_url_expires_at" timestamptz,
  "scanned_at" timestamptz,
  "attached_at" timestamptz,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "document_versions" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "document_id" uuid NOT NULL,
  "version_no" int NOT NULL,
  "storage_provider" varchar(20) NOT NULL DEFAULT 'minio',
  "bucket" varchar(255) NOT NULL,
  "file_path" varchar(1024) NOT NULL,
  "object_version" varchar(255),
  "etag" varchar(128),
  "original_filename" varchar(512),
  "mime_type" varchar(255),
  "size_bytes" bigint NOT NULL,
  "upload_status" varchar(30) NOT NULL DEFAULT 'UPLOADED',
  "uploaded_at" timestamptz NOT NULL DEFAULT (now()),
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "role_request_documents" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "request_id" bigint NOT NULL,
  "document_type_code" varchar(100) NOT NULL,
  "document_id" uuid NOT NULL,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "chat_rooms" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "code" varchar(64) UNIQUE NOT NULL,
  "name" varchar(255) NOT NULL,
  "room_type" varchar(16) NOT NULL DEFAULT 'GROUP',
  "direct_key" varchar(80),
  "last_message_seq" int NOT NULL DEFAULT 0,
  "last_message_at" timestamptz,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "chat_room_members" (
  "room_id" uuid NOT NULL,
  "user_id" uuid NOT NULL,
  "member_role" varchar(16) NOT NULL DEFAULT 'MEMBER',
  "last_read_seq" int NOT NULL DEFAULT 0,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255),
  PRIMARY KEY ("room_id", "user_id")
);

CREATE TABLE "chat_messages" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "room_id" uuid NOT NULL,
  "sender_id" uuid,
  "sender_username" varchar(254) NOT NULL,
  "body" text NOT NULL,
  "seq" int NOT NULL,
  "message_type" varchar(20) NOT NULL DEFAULT 'TEXT',
  "is_hidden" boolean NOT NULL DEFAULT false,
  "hidden_by" uuid,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "chat_message_attachments" (
  "message_id" uuid NOT NULL,
  "document_id" uuid NOT NULL,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255),
  PRIMARY KEY ("message_id", "document_id")
);

CREATE TABLE "chat_support_tickets" (
  "room_id" uuid PRIMARY KEY,
  "requester_id" uuid NOT NULL,
  "topic" varchar(50) NOT NULL,
  "order_id" uuid,
  "event_id" uuid,
  "status" varchar(20) NOT NULL DEFAULT 'OPEN',
  "flow_id" uuid NOT NULL,
  "current_step_id" uuid NOT NULL,
  "assigned_admin_id" uuid,
  "assigned_at" timestamptz,
  "first_response_at" timestamptz,
  "closed_at" timestamptz,
  "closed_by" varchar(255),
  "rating" smallint,
  "rating_comment" varchar(500),
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "canned_responses" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "title" varchar(50) NOT NULL,
  "shortcut" varchar(30) NOT NULL,
  "content" varchar(2000) NOT NULL,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "audit_logs" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "occurred_at" timestamptz NOT NULL DEFAULT (now()),
  "actor_id" uuid,
  "actor_name" varchar(255) NOT NULL,
  "action" varchar(50) NOT NULL,
  "entity_type" varchar(30) NOT NULL,
  "entity_id" varchar(64) NOT NULL,
  "before_data" jsonb,
  "after_data" jsonb,
  "reason" varchar(500),
  "trace_id" varchar(64),
  "ip_address" varchar(45)
);

CREATE TABLE "alerts" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "code" varchar(10) NOT NULL,
  "severity" varchar(50) NOT NULL,
  "message" varchar(500) NOT NULL,
  "entity_type" varchar(30),
  "entity_id" varchar(64),
  "status" varchar(20) NOT NULL DEFAULT 'OPEN',
  "acknowledged_by" uuid,
  "acknowledged_at" timestamptz,
  "resolved_at" timestamptz,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "inventory_reconciliations" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "ticket_type_id" uuid NOT NULL,
  "redis_available" int NOT NULL,
  "db_available" int NOT NULL,
  "diff" int NOT NULL,
  "action_taken" varchar(30),
  "checked_at" timestamptz NOT NULL DEFAULT (now())
);

CREATE TABLE "export_jobs" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "requested_by" uuid NOT NULL,
  "export_type" varchar(30) NOT NULL,
  "event_id" uuid,
  "status" varchar(20) NOT NULL DEFAULT 'QUEUED',
  "row_count" int,
  "document_id" uuid,
  "error" varchar(500),
  "finished_at" timestamptz,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "integration_outbox" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "aggregate_type" varchar(30) NOT NULL,
  "aggregate_id" uuid NOT NULL,
  "topic" varchar(255) NOT NULL,
  "message_key" varchar(255) NOT NULL,
  "event_type" varchar(50) NOT NULL,
  "payload" jsonb NOT NULL,
  "status" varchar(32) NOT NULL DEFAULT 'PENDING',
  "retry_count" int NOT NULL DEFAULT 0,
  "last_error" varchar(2000),
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "sent_at" timestamptz
);

CREATE TABLE "processed_events" (
  "consumer_group" varchar(100) NOT NULL,
  "event_id" uuid NOT NULL,
  "processed_at" timestamptz NOT NULL DEFAULT (now()),
  PRIMARY KEY ("consumer_group", "event_id")
);

CREATE TABLE "mail_templates" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "code" varchar(64) UNIQUE NOT NULL,
  "subject_template" varchar(500) NOT NULL,
  "body_html" text NOT NULL,
  "body_text" text,
  "is_active" boolean NOT NULL DEFAULT true,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE TABLE "mail_queue" (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "user_id" uuid,
  "to_address" varchar(320) NOT NULL,
  "template_code" varchar(64) NOT NULL,
  "variables" jsonb NOT NULL DEFAULT '{}',
  "ref_type" varchar(30),
  "ref_id" varchar(64),
  "status" varchar(20) NOT NULL DEFAULT 'PENDING',
  "attempt_count" int NOT NULL DEFAULT 0,
  "last_error" varchar(2000),
  "sent_at" timestamptz,
  "idempotency_key" varchar(128) UNIQUE NOT NULL,
  "is_deleted" boolean NOT NULL DEFAULT false,
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "created_by" varchar(255),
  "updated_at" timestamptz NOT NULL DEFAULT (now()),
  "updated_by" varchar(255)
);

CREATE INDEX "idx_system_configs_group_code" ON "system_configs" ("group_code", "code");

CREATE INDEX "idx_system_configs_group_order" ON "system_configs" ("group_code", "order_no");

CREATE INDEX "idx_events_organizer_status" ON "events" ("organizer_id", "status");

CREATE INDEX "idx_events_status_sale_start" ON "events" ("status", "sale_starts_at");

CREATE INDEX "idx_events_province_category" ON "events" ("province_id", "category");

CREATE UNIQUE INDEX "uq_event_documents_event_doc" ON "event_documents" ("event_id", "document_id");

CREATE UNIQUE INDEX "uq_ticket_types_id_event" ON "ticket_types" ("id", "event_id");

CREATE INDEX "idx_ticket_types_event" ON "ticket_types" ("event_id");

CREATE UNIQUE INDEX "uq_reservations_order_match" ON "reservations" ("id", "user_id", "event_id", "ticket_type_id", "quantity");

CREATE INDEX "idx_reservations_user_event" ON "reservations" ("user_id", "event_id");

CREATE INDEX "idx_reservations_ticket_type" ON "reservations" ("ticket_type_id");

CREATE INDEX "idx_reservations_event_status" ON "reservations" ("event_id", "status");

CREATE UNIQUE INDEX "uq_orders_ticket_match" ON "orders" ("id", "user_id", "event_id", "ticket_type_id");

CREATE UNIQUE INDEX "uq_orders_id_amount" ON "orders" ("id", "total_amount");

CREATE UNIQUE INDEX "uq_orders_id_user" ON "orders" ("id", "user_id");

CREATE UNIQUE INDEX "uq_orders_order_code" ON "orders" ("order_code");

CREATE UNIQUE INDEX "uq_orders_user_idem" ON "orders" ("user_id", "idempotency_key");

CREATE INDEX "idx_orders_user_created" ON "orders" ("user_id", "created_at");

CREATE INDEX "idx_orders_event_status" ON "orders" ("event_id", "status");

CREATE INDEX "idx_orders_status_created" ON "orders" ("status", "created_at");

CREATE INDEX "idx_orders_reservation" ON "orders" ("reservation_id");

CREATE UNIQUE INDEX "uq_payment_gateway_txn" ON "payment_transactions" ("gateway_txn_id");

CREATE INDEX "idx_payment_order" ON "payment_transactions" ("order_id");

CREATE INDEX "idx_callback_txn" ON "payment_callback_logs" ("gateway_txn_id");

CREATE INDEX "idx_callback_received" ON "payment_callback_logs" ("received_at");

CREATE UNIQUE INDEX "uq_tickets_code" ON "tickets" ("ticket_code");

CREATE INDEX "idx_tickets_order" ON "tickets" ("order_id");

CREATE INDEX "idx_tickets_user_event" ON "tickets" ("user_id", "event_id");

CREATE INDEX "idx_tickets_event_status" ON "tickets" ("event_id", "status");

CREATE UNIQUE INDEX "uq_refunds_order" ON "refunds" ("order_id");

CREATE UNIQUE INDEX "uq_flow_steps_flow_code" ON "flow_steps" ("flow_id", "code");

CREATE UNIQUE INDEX "uq_flow_steps_flow_id_code" ON "flow_steps" ("flow_id", "id", "code");

CREATE UNIQUE INDEX "uq_event_flows_step" ON "event_flows" ("event_id", "step_no");

CREATE INDEX "idx_event_flows_processed_by" ON "event_flows" ("processed_by", "processed_at");

CREATE UNIQUE INDEX "uq_order_flows_step" ON "order_flows" ("order_id", "step_no");

CREATE INDEX "idx_order_flows_processed_by" ON "order_flows" ("processed_by", "processed_at");

CREATE UNIQUE INDEX "uq_refund_flows_step" ON "refund_flows" ("refund_id", "step_no");

CREATE INDEX "idx_refund_flows_processed_by" ON "refund_flows" ("processed_by", "processed_at");

CREATE UNIQUE INDEX "uq_support_ticket_flows_step" ON "support_ticket_flows" ("room_id", "step_no");

CREATE INDEX "idx_support_ticket_flows_processed_by" ON "support_ticket_flows" ("processed_by", "processed_at");

CREATE UNIQUE INDEX "uq_documents_provider_bucket_path" ON "documents" ("provider", "bucket", "file_path");

CREATE UNIQUE INDEX "uq_documents_id_type" ON "documents" ("id", "document_type_code");

CREATE INDEX "idx_documents_owner_status" ON "documents" ("owner_id", "status");

CREATE UNIQUE INDEX "uq_document_versions_doc_version" ON "document_versions" ("document_id", "version_no");

CREATE UNIQUE INDEX "uq_document_versions_storage_path" ON "document_versions" ("storage_provider", "bucket", "file_path");

CREATE UNIQUE INDEX "uq_role_request_documents_req_doc" ON "role_request_documents" ("request_id", "document_id");

CREATE INDEX "idx_chat_room_members_user" ON "chat_room_members" ("user_id", "room_id");

CREATE UNIQUE INDEX "uq_chat_messages_room_seq" ON "chat_messages" ("room_id", "seq");

CREATE INDEX "idx_chat_messages_room_created" ON "chat_messages" ("room_id", "created_at");

CREATE INDEX "idx_support_requester_status" ON "chat_support_tickets" ("requester_id", "status");

CREATE INDEX "idx_support_status_created" ON "chat_support_tickets" ("status", "created_at");

CREATE INDEX "idx_support_admin_status" ON "chat_support_tickets" ("assigned_admin_id", "status");

CREATE INDEX "idx_audit_entity" ON "audit_logs" ("entity_type", "entity_id");

CREATE INDEX "idx_audit_actor" ON "audit_logs" ("actor_id", "occurred_at");

CREATE INDEX "idx_audit_occurred" ON "audit_logs" ("occurred_at");

CREATE INDEX "idx_alerts_status_severity" ON "alerts" ("status", "severity");

CREATE INDEX "idx_recon_ticket_checked" ON "inventory_reconciliations" ("ticket_type_id", "checked_at");

CREATE INDEX "idx_mail_queue_ref" ON "mail_queue" ("ref_type", "ref_id");

COMMENT ON TABLE "system_configs" IS 'Danh mục dùng chung và tham số hệ thống, theo mẫu system_config của hệ thống cũ (group_code, code, label, value, order_no, is_active). Bảng nghiệp vụ lưu MÃ (events.category = MUSIC). KHÔNG dùng cho trạng thái nghiệp vụ.
| group_code | code | label | value | order_no | is_active |
|---|---|---|---|---|---|
| GENDER | MALE | Nam |  | 1 | true |
| EVENT_CATEGORY | MUSIC | Âm nhạc |  | 1 | true |
| SETTING | HOLD_MINUTES | Thời gian giữ chỗ mặc định (phút) | 10 | 1 | true |
';

COMMENT ON COLUMN "system_configs"."group_code" IS 'Nhóm danh mục: GENDER, EVENT_CATEGORY, CHAT_TOPIC, REFUND_REASON, ALERT_SEVERITY, SETTING...';

COMMENT ON COLUMN "system_configs"."code" IS 'Mã giá trị trong nhóm: MALE, MUSIC, HOLD_MINUTES... Bảng nghiệp vụ lưu mã này';

COMMENT ON COLUMN "system_configs"."label" IS 'Tên hiển thị: Nam, Âm nhạc, Thời gian giữ chỗ (phút)';

COMMENT ON COLUMN "system_configs"."value" IS 'Chỉ dùng cho nhóm SETTING: giá trị tham số, ví dụ 10';

COMMENT ON COLUMN "system_configs"."order_no" IS 'Thứ tự hiển thị trong dropdown';

COMMENT ON COLUMN "system_configs"."is_active" IS 'Tắt một giá trị để không cho chọn mới, dữ liệu cũ vẫn giữ';

COMMENT ON COLUMN "system_configs"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "provinces" IS 'Danh mục 34 tỉnh, thành phố (từ 01/07/2025, không còn cấp huyện), theo mẫu provinces của hệ thống cũ: khóa uuid + gso_code.
| id | gso_code | name | short_name | type |
|---|---|---|---|---|
| uuid-01 | 01 | Thành phố Hà Nội | Hà Nội | CITY |
| uuid-79 | 79 | Thành phố Hồ Chí Minh | Hồ Chí Minh | CITY |
| uuid-04 | 04 | Tỉnh Cao Bằng | Cao Bằng | PROVINCE |
';

COMMENT ON COLUMN "provinces"."gso_code" IS 'Mã GSO 2 chữ số theo Quyết định 19/2025/QĐ-TTg, ví dụ 01 = Hà Nội, 79 = TP Hồ Chí Minh';

COMMENT ON COLUMN "provinces"."name" IS 'Tên đầy đủ: Thành phố Hà Nội, Tỉnh Cao Bằng';

COMMENT ON COLUMN "provinces"."short_name" IS 'Tên ngắn để hiển thị và lọc: Hà Nội, Cao Bằng';

COMMENT ON COLUMN "provinces"."type" IS 'CITY (thành phố trực thuộc trung ương) | PROVINCE (tỉnh)';

COMMENT ON COLUMN "provinces"."order_no" IS 'Thứ tự hiển thị';

COMMENT ON COLUMN "provinces"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "events" IS 'Sự kiện do ban tổ chức tạo, theo mẫu opportunities của hệ thống cũ (code, flow_id, current_step_id). Phải được Admin duyệt mới công khai; ON_SALE / SALE_ENDED / COMPLETED do job tự chuyển theo giờ. Quy trình ở flows / flow_steps (EVENT_LIFECYCLE), lịch sử ở event_flows, tệp ở event_documents.
| id | code | name | organizer_id | province_id | flow_id | current_step_id | status |
|---|---|---|---|---|---|---|---|
| uuid01 | EV-000001 | Đêm nhạc Mùa Đông 2026 | user-btc-01 | uuid-01 | EVENT_LIFECYCLE | step-on-sale | ON_SALE |
';

COMMENT ON COLUMN "events"."code" IS 'Mã sự kiện, tự sinh, chỉ đọc, ví dụ EV-000001';

COMMENT ON COLUMN "events"."organizer_id" IS 'Ban tổ chức sở hữu sự kiện';

COMMENT ON COLUMN "events"."description" IS 'Bắt buộc khi gửi duyệt, 20-10.000 ký tự';

COMMENT ON COLUMN "events"."category" IS 'Mã trong system_configs nhóm EVENT_CATEGORY: MUSIC, SPORT, CONFERENCE, THEATER, OTHER...';

COMMENT ON COLUMN "events"."venue_name" IS 'Tên địa điểm';

COMMENT ON COLUMN "events"."venue_address" IS 'Số nhà, đường, phường/xã (chữ tự do)';

COMMENT ON COLUMN "events"."province_id" IS 'Tỉnh/thành nơi tổ chức; bắt buộc khi gửi duyệt; dùng để lọc';

COMMENT ON COLUMN "events"."starts_at" IS 'Giờ sự kiện bắt đầu diễn ra';

COMMENT ON COLUMN "events"."ends_at" IS 'Giờ sự kiện kết thúc';

COMMENT ON COLUMN "events"."waiting_room_opens_at" IS 'Giờ mở phòng chờ, trước giờ mở bán 5-60 phút';

COMMENT ON COLUMN "events"."sale_starts_at" IS 'Giờ mở bán';

COMMENT ON COLUMN "events"."sale_ends_at" IS 'Giờ đóng bán';

COMMENT ON COLUMN "events"."max_tickets_per_user" IS 'Giới hạn vé mỗi tài khoản trên toàn sự kiện, 1-10 (EVT-BR-03, RSV-BR-01)';

COMMENT ON COLUMN "events"."hold_minutes" IS 'Thời gian giữ chỗ (RSV-BR-03)';

COMMENT ON COLUMN "events"."admission_rate" IS 'Tốc độ cấp lượt vào từ phòng chờ, người/giây (QUE-FR-05)';

COMMENT ON COLUMN "events"."status" IS 'DRAFT | PENDING_REVIEW | REJECTED | APPROVED | ON_SALE | PAUSED | SALE_ENDED | CANCELLED | COMPLETED';

COMMENT ON COLUMN "events"."flow_id" IS 'Quy trình áp dụng (EVENT_LIFECYCLE); status phải là code của một bước trong quy trình này';

COMMENT ON COLUMN "events"."current_step_id" IS 'Bước hiện tại trong quy trình (giống opportunities.current_step_id). Khóa ngoại ghép (flow_id, current_step_id, status) bảo đảm bước thuộc đúng quy trình và trùng status';

COMMENT ON COLUMN "events"."submitted_at" IS 'Lúc gửi duyệt';

COMMENT ON COLUMN "events"."reviewed_by" IS 'Admin duyệt hoặc từ chối LẦN GẦN NHẤT; lịch sử đầy đủ ở flow_steps';

COMMENT ON COLUMN "events"."reject_reason" IS 'Bắt buộc khi REJECTED; lý do lần gần nhất, các lần trước ở flow_steps';

COMMENT ON COLUMN "events"."pause_count" IS 'Số lần ban tổ chức đã tạm dừng, tối đa 3 (EVT-BR-07)';

COMMENT ON COLUMN "events"."paused_at" IS 'Lúc bắt đầu lần tạm dừng hiện tại';

COMMENT ON COLUMN "events"."cancel_reason" IS 'Bắt buộc khi CANCELLED';

COMMENT ON COLUMN "events"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "event_documents" IS 'Tệp gắn với sự kiện, theo mẫu opportunity_documents của hệ thống cũ. Mỗi sự kiện tối đa 1 ảnh bìa (unique index có điều kiện ở mục 6). Thay cho events.cover_document_id và bảng event_images.
| id | event_id | document_type_code | document_id | sort_order |
|---|---|---|---|---|
| uuid01 | EV-000001 | EVENT_COVER | doc-cover | 0 |
| uuid02 | EV-000001 | EVENT_IMAGE | doc-stage | 1 |
';

COMMENT ON COLUMN "event_documents"."event_id" IS 'Sự kiện';

COMMENT ON COLUMN "event_documents"."document_type_code" IS 'EVENT_COVER (ảnh bìa, tối đa 1) | EVENT_IMAGE (ảnh phụ, tối đa 10); phải trùng loại của tệp';

COMMENT ON COLUMN "event_documents"."document_id" IS 'Tệp trong bảng documents (khóa ngoại ghép cùng document_type_code)';

COMMENT ON COLUMN "event_documents"."sort_order" IS 'Thứ tự hiển thị ảnh phụ';

COMMENT ON COLUMN "event_documents"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "ticket_types" IS 'Loại vé của sự kiện. Đây là NGUỒN SỰ THẬT của tồn kho: còn lại = total_quantity - held_quantity - sold_quantity. Redis chỉ giữ bản nóng trong đợt bán.';

COMMENT ON COLUMN "ticket_types"."name" IS 'Ví dụ VIP, Thường; không trùng trong một sự kiện';

COMMENT ON COLUMN "ticket_types"."price" IS 'Giá một vé (VND), 0 = miễn phí';

COMMENT ON COLUMN "ticket_types"."total_quantity" IS 'Tổng số vé phát hành';

COMMENT ON COLUMN "ticket_types"."held_quantity" IS 'Số vé đang giữ chỗ chờ thanh toán';

COMMENT ON COLUMN "ticket_types"."sold_quantity" IS 'Số vé đã bán (đơn PAID/ISSUED)';

COMMENT ON COLUMN "ticket_types"."sale_paused" IS 'Tự bật khi đối soát phát hiện lệch tồn kho (RSV-BR-08)';

COMMENT ON COLUMN "ticket_types"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "reservations" IS 'Giữ chỗ: vé được trừ tạm khỏi kho cho một người trong thời gian chờ thanh toán. Ghi từ sự kiện Kafka reservation.created sau khi Lua script trên Redis trừ kho thành công. Index cho job quét là index có điều kiện ở mục 6.';

COMMENT ON COLUMN "reservations"."status" IS 'HELD | CONFIRMED | CANCELLED | EXPIRED';

COMMENT ON COLUMN "reservations"."expires_at" IS 'Hạn giữ chỗ';

COMMENT ON COLUMN "reservations"."extended" IS 'Đã gia hạn 5 phút khi đang thanh toán chưa; chỉ 1 lần (RSV-BR-03)';

COMMENT ON COLUMN "reservations"."confirmed_at" IS 'Lúc chốt khi đơn thanh toán xong';

COMMENT ON COLUMN "reservations"."released_at" IS 'Lúc trả vé về kho';

COMMENT ON COLUMN "reservations"."release_reason" IS 'USER_CANCELLED | EXPIRED | ACCOUNT_LOCKED | EVENT_CANCELLED';

COMMENT ON COLUMN "reservations"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "orders" IS 'Đơn hàng tạo từ một giữ chỗ. Quy trình ở flows / flow_steps (ORDER_LIFECYCLE), lịch sử ở order_flows. Vì mỗi giữ chỗ chỉ có một loại vé nên đơn lưu thẳng loại vé và số lượng, không cần bảng order_items. KHÔNG xóa, kể cả xóa mềm (is_deleted luôn false, có CHECK).';

COMMENT ON COLUMN "orders"."order_code" IS 'Mã đơn hiển thị cho khách, ví dụ FS-000123';

COMMENT ON COLUMN "orders"."unit_price" IS 'Giá chốt lúc tạo đơn, lấy từ DB (PAY-BR-06)';

COMMENT ON COLUMN "orders"."total_amount" IS '= unit_price x quantity';

COMMENT ON COLUMN "orders"."status" IS 'PENDING_PAYMENT | PAID | ISSUED | CANCELLED | REFUNDING | REFUNDED';

COMMENT ON COLUMN "orders"."flow_id" IS 'Quy trình áp dụng (ORDER_LIFECYCLE); status phải là code của một bước trong quy trình này';

COMMENT ON COLUMN "orders"."current_step_id" IS 'Bước hiện tại trong quy trình (giống opportunities.current_step_id). Khóa ngoại ghép (flow_id, current_step_id, status) bảo đảm bước thuộc đúng quy trình và trùng status';

COMMENT ON COLUMN "orders"."idempotency_key" IS 'Do client sinh; bấm thanh toán nhiều lần vẫn một đơn';

COMMENT ON COLUMN "orders"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "payment_transactions" IS 'Giao dịch thanh toán tại cổng. Một đơn thường có một giao dịch. KHÔNG xóa, kể cả xóa mềm (is_deleted luôn false, có CHECK). Index cho job quét là index có điều kiện ở mục 6.';

COMMENT ON COLUMN "payment_transactions"."gateway" IS 'MOCK | VNPAY | MOMO';

COMMENT ON COLUMN "payment_transactions"."gateway_txn_id" IS 'Mã giao dịch phía cổng; mỗi mã chỉ xử lý một lần (PAY-BR-03)';

COMMENT ON COLUMN "payment_transactions"."amount" IS 'Số tiền yêu cầu thanh toán';

COMMENT ON COLUMN "payment_transactions"."paid_amount" IS 'Số tiền cổng báo về; phải khớp amount (PAY-BR-02)';

COMMENT ON COLUMN "payment_transactions"."status" IS 'INITIATED | SUCCEEDED | FAILED | EXPIRED';

COMMENT ON COLUMN "payment_transactions"."last_queried_at" IS 'Lần gần nhất job chủ động hỏi cổng';

COMMENT ON COLUMN "payment_transactions"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "payment_callback_logs" IS 'Nhật ký mọi callback nhận từ cổng, kể cả trùng và sai chữ ký. Chỉ thêm.';

COMMENT ON COLUMN "payment_callback_logs"."gateway_txn_id" IS 'Có thể null nếu payload hỏng';

COMMENT ON COLUMN "payment_callback_logs"."payload" IS 'Nguyên văn callback';

COMMENT ON COLUMN "payment_callback_logs"."result" IS 'PROCESSED | DUPLICATE | INVALID_SIGNATURE | AMOUNT_MISMATCH | ORDER_NOT_FOUND';

COMMENT ON TABLE "tickets" IS 'Vé điện tử. Mỗi vé một dòng, một mã QR. Vé bị vô hiệu (REVOKED) khi đơn được hoàn tiền. KHÔNG xóa, kể cả xóa mềm (is_deleted luôn false, có CHECK).';

COMMENT ON COLUMN "tickets"."user_id" IS 'Chủ vé';

COMMENT ON COLUMN "tickets"."ticket_code" IS 'Mã vé ngẫu nhiên, nằm trong QR';

COMMENT ON COLUMN "tickets"."qr_signature" IS 'Chữ ký HMAC của ticket_code, chống làm giả (TKT-BR-01)';

COMMENT ON COLUMN "tickets"."status" IS 'VALID | REVOKED';

COMMENT ON COLUMN "tickets"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "refunds" IS 'Yêu cầu hoàn tiền: tự động (tiền về muộn khi hết vé, sự kiện bị hủy, lệch số tiền) hoặc do Admin làm thủ công. KHÔNG xóa, kể cả xóa mềm (is_deleted luôn false, có CHECK). Index cho job quét là index có điều kiện ở mục 6.';

COMMENT ON COLUMN "refunds"."order_id" IS 'Mỗi đơn chỉ hoàn một lần (RFD-BR-02)';

COMMENT ON COLUMN "refunds"."amount" IS 'Luôn bằng 100% số tiền đã trả (RFD-BR-01)';

COMMENT ON COLUMN "refunds"."reason_type" IS 'Mã trong system_configs nhóm REFUND_REASON: LATE_PAYMENT, EVENT_CANCELLED, AMOUNT_MISMATCH, MANUAL';

COMMENT ON COLUMN "refunds"."status" IS 'REQUESTED | PROCESSING | COMPLETED | FAILED';

COMMENT ON COLUMN "refunds"."flow_id" IS 'Quy trình áp dụng (REFUND_PROCESS); status phải là code của một bước trong quy trình này';

COMMENT ON COLUMN "refunds"."current_step_id" IS 'Bước hiện tại trong quy trình (giống opportunities.current_step_id). Khóa ngoại ghép (flow_id, current_step_id, status) bảo đảm bước thuộc đúng quy trình và trùng status';

COMMENT ON COLUMN "refunds"."retry_count" IS 'Tối đa 5 lần rồi cảnh báo Admin (RFD-FR-04)';

COMMENT ON COLUMN "refunds"."requested_by" IS 'Admin khi hoàn thủ công; null khi hệ thống tự hoàn';

COMMENT ON COLUMN "refunds"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "flows" IS 'ĐỊNH NGHĨA quy trình (dữ liệu danh mục), theo mẫu flows của hệ thống cũ. Bảng nghiệp vụ trỏ tới đây qua flow_id, giống opportunities.flow_id.
| id | code | name | entity_type |
|---|---|---|---|
| flow-ev-01 | EVENT_LIFECYCLE | Quy trình sự kiện | EVENT |
| flow-or-01 | ORDER_LIFECYCLE | Quy trình đơn hàng | ORDER |
';

COMMENT ON COLUMN "flows"."code" IS 'EVENT_LIFECYCLE | ORDER_LIFECYCLE | REFUND_PROCESS | SUPPORT_TICKET';

COMMENT ON COLUMN "flows"."name" IS 'Tên quy trình, ví dụ Quy trình sự kiện';

COMMENT ON COLUMN "flows"."entity_type" IS 'EVENT | ORDER | REFUND | SUPPORT_TICKET: loại đối tượng dùng quy trình này';

COMMENT ON COLUMN "flows"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "flow_steps" IS 'ĐỊNH NGHĨA các bước của quy trình, theo mẫu flow_steps của hệ thống cũ. code trùng mã status của bảng nghiệp vụ. Quy tắc bước nào được chuyển sang bước nào nằm trong code Java.
| id | flow_id | code | name | step_order | is_initial | is_final |
|---|---|---|---|---|---|---|
| step-ev-1 | flow-ev-01 | DRAFT | Nháp | 1 | true | false |
| step-ev-2 | flow-ev-01 | PENDING_REVIEW | Chờ duyệt | 2 | false | false |
| step-ev-3 | flow-ev-01 | REJECTED | Bị từ chối | 3 | false | false |
';

COMMENT ON COLUMN "flow_steps"."code" IS 'Mã bước, TRÙNG mã status của bảng nghiệp vụ: DRAFT, PENDING_REVIEW, PAID...';

COMMENT ON COLUMN "flow_steps"."name" IS 'Tên hiển thị: Nháp, Chờ duyệt, Đã thanh toán...';

COMMENT ON COLUMN "flow_steps"."step_order" IS 'Thứ tự hiển thị trên dòng thời gian';

COMMENT ON COLUMN "flow_steps"."is_initial" IS 'Bước bắt đầu của quy trình';

COMMENT ON COLUMN "flow_steps"."is_final" IS 'Bước kết thúc';

COMMENT ON COLUMN "flow_steps"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "event_flows" IS 'Lịch sử chuyển bước của sự kiện, theo mẫu opportunity_flows của hệ thống cũ. Chỉ thêm, không sửa, không xóa.
| id | event_id | step_no | from_step_id | flow_step_id | action | processed_by_name | reason |
|---|---|---|---|---|---|---|---|
| uuid01 | EV-000004 | 1 |  | DRAFT | CREATE | organizer@... |  |
| uuid02 | EV-000004 | 2 | DRAFT | PENDING_REVIEW | SUBMIT | organizer@... |  |
| uuid03 | EV-000004 | 3 | PENDING_REVIEW | REJECTED | REJECT | admin@... | Thiếu ảnh bìa và giấy phép |
';

COMMENT ON COLUMN "event_flows"."event_id" IS 'Sự kiện';

COMMENT ON COLUMN "event_flows"."step_no" IS 'Lần chuyển thứ mấy của đối tượng này: 1, 2, 3...';

COMMENT ON COLUMN "event_flows"."from_step_id" IS 'Bước trước khi chuyển; null ở lần đầu';

COMMENT ON COLUMN "event_flows"."flow_step_id" IS 'Bước đạt tới sau lần chuyển này; code của bước trùng status mới của bảng chính';

COMMENT ON COLUMN "event_flows"."action" IS 'CREATE | SUBMIT | APPROVE | REJECT | PAY | ISSUE | REFUND | CLAIM | CLOSE ...';

COMMENT ON COLUMN "event_flows"."processed_by" IS 'Người thao tác; null khi hệ thống tự làm';

COMMENT ON COLUMN "event_flows"."processed_by_name" IS 'Email người thao tác hoặc SYSTEM';

COMMENT ON COLUMN "event_flows"."reason" IS 'Bắt buộc với REJECT, CANCEL, hoàn tiền thủ công';

COMMENT ON COLUMN "event_flows"."metadata" IS 'Dữ liệu kèm theo: mã giao dịch, lỗi cổng, Admin được chuyển...';

COMMENT ON TABLE "order_flows" IS 'Lịch sử các bước của đơn hàng theo quy trình ORDER_LIFECYCLE. Chỉ thêm, không sửa, không xóa.';

COMMENT ON COLUMN "order_flows"."order_id" IS 'Đơn hàng';

COMMENT ON COLUMN "order_flows"."step_no" IS 'Lần chuyển thứ mấy của đối tượng này: 1, 2, 3...';

COMMENT ON COLUMN "order_flows"."from_step_id" IS 'Bước trước khi chuyển; null ở lần đầu';

COMMENT ON COLUMN "order_flows"."flow_step_id" IS 'Bước đạt tới sau lần chuyển này; code của bước trùng status mới của bảng chính';

COMMENT ON COLUMN "order_flows"."action" IS 'CREATE | SUBMIT | APPROVE | REJECT | PAY | ISSUE | REFUND | CLAIM | CLOSE ...';

COMMENT ON COLUMN "order_flows"."processed_by" IS 'Người thao tác; null khi hệ thống tự làm';

COMMENT ON COLUMN "order_flows"."processed_by_name" IS 'Email người thao tác hoặc SYSTEM';

COMMENT ON COLUMN "order_flows"."reason" IS 'Bắt buộc với REJECT, CANCEL, hoàn tiền thủ công';

COMMENT ON COLUMN "order_flows"."metadata" IS 'Dữ liệu kèm theo: mã giao dịch, lỗi cổng, Admin được chuyển...';

COMMENT ON TABLE "refund_flows" IS 'Lịch sử các bước của yêu cầu hoàn tiền theo quy trình REFUND_PROCESS. Chỉ thêm, không sửa, không xóa.';

COMMENT ON COLUMN "refund_flows"."refund_id" IS 'Yêu cầu hoàn tiền';

COMMENT ON COLUMN "refund_flows"."step_no" IS 'Lần chuyển thứ mấy của đối tượng này: 1, 2, 3...';

COMMENT ON COLUMN "refund_flows"."from_step_id" IS 'Bước trước khi chuyển; null ở lần đầu';

COMMENT ON COLUMN "refund_flows"."flow_step_id" IS 'Bước đạt tới sau lần chuyển này; code của bước trùng status mới của bảng chính';

COMMENT ON COLUMN "refund_flows"."action" IS 'CREATE | SUBMIT | APPROVE | REJECT | PAY | ISSUE | REFUND | CLAIM | CLOSE ...';

COMMENT ON COLUMN "refund_flows"."processed_by" IS 'Người thao tác; null khi hệ thống tự làm';

COMMENT ON COLUMN "refund_flows"."processed_by_name" IS 'Email người thao tác hoặc SYSTEM';

COMMENT ON COLUMN "refund_flows"."reason" IS 'Bắt buộc với REJECT, CANCEL, hoàn tiền thủ công';

COMMENT ON COLUMN "refund_flows"."metadata" IS 'Dữ liệu kèm theo: mã giao dịch, lỗi cổng, Admin được chuyển...';

COMMENT ON TABLE "support_ticket_flows" IS 'Lịch sử các bước của hội thoại hỗ trợ theo quy trình SUPPORT_TICKET. Chỉ thêm, không sửa, không xóa.';

COMMENT ON COLUMN "support_ticket_flows"."room_id" IS 'Hội thoại hỗ trợ';

COMMENT ON COLUMN "support_ticket_flows"."step_no" IS 'Lần chuyển thứ mấy của đối tượng này: 1, 2, 3...';

COMMENT ON COLUMN "support_ticket_flows"."from_step_id" IS 'Bước trước khi chuyển; null ở lần đầu';

COMMENT ON COLUMN "support_ticket_flows"."flow_step_id" IS 'Bước đạt tới sau lần chuyển này; code của bước trùng status mới của bảng chính';

COMMENT ON COLUMN "support_ticket_flows"."action" IS 'CREATE | SUBMIT | APPROVE | REJECT | PAY | ISSUE | REFUND | CLAIM | CLOSE ...';

COMMENT ON COLUMN "support_ticket_flows"."processed_by" IS 'Người thao tác; null khi hệ thống tự làm';

COMMENT ON COLUMN "support_ticket_flows"."processed_by_name" IS 'Email người thao tác hoặc SYSTEM';

COMMENT ON COLUMN "support_ticket_flows"."reason" IS 'Bắt buộc với REJECT, CANCEL, hoàn tiền thủ công';

COMMENT ON COLUMN "support_ticket_flows"."metadata" IS 'Dữ liệu kèm theo: mã giao dịch, lỗi cổng, Admin được chuyển...';

COMMENT ON TABLE "document_types" IS 'Danh mục loại tệp và quy tắc của từng loại (dung lượng, định dạng, có phải quét không). Giữ nguyên cấu trúc bảng document_types của sale-app, thêm 2 cột.';

COMMENT ON COLUMN "document_types"."code" IS 'Mã loại, các bảng khác trỏ tới bằng mã này: AVATAR | EVENT_COVER | EVENT_IMAGE | ORG_BUSINESS_LICENSE | ORG_REPRESENTATIVE_ID | ORG_OTHER | CHAT_ATTACHMENT | REPORT_EXPORT';

COMMENT ON COLUMN "document_types"."require_verification" IS 'true = phải kiểm tra magic bytes và quét mã độc trước khi dùng';

COMMENT ON COLUMN "document_types"."max_size_mb" IS 'Dung lượng tối đa một tệp (MB)';

COMMENT ON COLUMN "document_types"."allowed_mime_pattern" IS 'Biểu thức chính quy cho mime được phép, ví dụ ^image/(jpeg|png|webp)$';

COMMENT ON COLUMN "document_types"."max_files_per_ref" IS 'THÊM MỚI so với sale-app. Số tệp tối đa gắn vào một đối tượng: 1 ảnh bìa, 10 ảnh phụ, 5 tệp một tin nhắn';

COMMENT ON COLUMN "document_types"."default_visibility" IS 'THÊM MỚI. PUBLIC | PRIVATE';

COMMENT ON COLUMN "document_types"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "documents" IS 'Tệp đã tải lên (ảnh sự kiện, giấy tờ, đính kèm chat, ảnh đại diện, file báo cáo). Giữ nguyên các cột của bảng documents trong sale-app, thêm các cột phục vụ phân quyền và quét mã độc. Tệp gắn vào đối tượng CHỈ qua bảng nối (event_documents, chat_message_attachments, role_request_documents) hoặc cột trỏ thẳng (export_jobs.document_id); bảng này không lưu ngược đối tượng để tránh hai nguồn lệch nhau. Tệp mồ côi = status READY quá 24 giờ. Index cho job quét là index có điều kiện ở mục 6.';

COMMENT ON COLUMN "documents"."bucket" IS 'public-assets | private-files';

COMMENT ON COLUMN "documents"."file_path" IS 'Đường dẫn object trên MinIO, tên là UUID ngẫu nhiên (FILE-BR-02)';

COMMENT ON COLUMN "documents"."original_filename" IS 'Tên gốc đã lọc ký tự đặc biệt, chỉ để hiển thị';

COMMENT ON COLUMN "documents"."mime_type" IS 'Kiểu thật sau khi kiểm tra magic bytes';

COMMENT ON COLUMN "documents"."size_bytes" IS 'Dung lượng (byte)';

COMMENT ON COLUMN "documents"."status" IS 'PENDING (đã cấp link) | UPLOADED (đã tải lên, đang kiểm tra) | READY | ATTACHED | REJECTED | QUARANTINED | DELETED | EXPIRED';

COMMENT ON COLUMN "documents"."document_type_code" IS 'Loại tệp (mã), giống role_requests.role_code trỏ authz.roles.code';

COMMENT ON COLUMN "documents"."current_version_id" IS 'Phiên bản đang dùng';

COMMENT ON COLUMN "documents"."owner_id" IS 'THÊM MỚI. Người tải lên, dùng để kiểm tra quyền và hạn mức';

COMMENT ON COLUMN "documents"."visibility" IS 'THÊM MỚI. PUBLIC | PRIVATE. Ảnh sự kiện chỉ thành PUBLIC khi sự kiện được duyệt (FILE-BR-04)';

COMMENT ON COLUMN "documents"."reject_reason" IS 'THÊM MỚI. Lý do REJECTED hoặc tên mã độc khi QUARANTINED';

COMMENT ON COLUMN "documents"."variants" IS 'THÊM MỚI. Bản thu nhỏ của ảnh, ví dụ {"w200": "...", "w800": "..."}';

COMMENT ON COLUMN "documents"."upload_url_expires_at" IS 'THÊM MỚI. Hạn presigned URL (5 phút)';

COMMENT ON COLUMN "documents"."scanned_at" IS 'THÊM MỚI';

COMMENT ON COLUMN "documents"."attached_at" IS 'THÊM MỚI. Lúc gắn vào đối tượng (status chuyển ATTACHED)';

COMMENT ON COLUMN "documents"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "document_versions" IS 'Lịch sử phiên bản của tệp, giữ nguyên như sale-app. Dùng khi thay ảnh bìa hoặc ảnh đại diện: tạo phiên bản mới, documents.current_version_id trỏ sang bản mới.';

COMMENT ON COLUMN "document_versions"."object_version" IS 'Version id của object trên MinIO nếu bật versioning';

COMMENT ON COLUMN "document_versions"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "role_request_documents" IS 'Giấy tờ đính kèm yêu cầu vai trò (ví dụ xin làm ban tổ chức), theo mẫu opportunity_documents: có loại giấy tờ trên bảng nối, và loại này phải trùng loại của tệp (khóa ngoại ghép).';

COMMENT ON COLUMN "role_request_documents"."request_id" IS 'bigint vì role_requests.id là bigserial';

COMMENT ON COLUMN "role_request_documents"."document_type_code" IS 'ORG_BUSINESS_LICENSE (giấy phép kinh doanh) | ORG_REPRESENTATIVE_ID (giấy tờ người đại diện) | ORG_OTHER; phải trùng loại của tệp';

COMMENT ON COLUMN "role_request_documents"."document_id" IS 'Khóa ngoại ghép cùng document_type_code';

COMMENT ON COLUMN "role_request_documents"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "chat_rooms" IS 'Phòng chat. Giữ nguyên cấu trúc sale-app (code, name, room_type, direct_key), thêm loại SUPPORT và 2 cột theo dõi tin mới nhất. Mỗi hội thoại hỗ trợ là một phòng SUPPORT.';

COMMENT ON COLUMN "chat_rooms"."code" IS 'Mã phòng, dùng trong topic STOMP /topic/chat/{code}. Phòng hỗ trợ: sp_<uuid>';

COMMENT ON COLUMN "chat_rooms"."room_type" IS 'GROUP | DIRECT | SUPPORT. SUPPORT là giá trị THÊM MỚI cho hội thoại hỗ trợ';

COMMENT ON COLUMN "chat_rooms"."direct_key" IS 'Chỉ dùng cho DIRECT, như sale-app';

COMMENT ON COLUMN "chat_rooms"."last_message_seq" IS 'THÊM MỚI. Số thứ tự tin mới nhất trong phòng';

COMMENT ON COLUMN "chat_rooms"."last_message_at" IS 'THÊM MỚI';

COMMENT ON COLUMN "chat_rooms"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "chat_room_members" IS 'Thành viên của phòng chat, như sale-app; interceptor dựa vào bảng này để cho phép subscribe. Phòng SUPPORT có 1 REQUESTER, thêm AGENT khi Admin nhận xử lý. Admin chuyển hội thoại thì xóa mềm AGENT cũ và thêm AGENT mới.';

COMMENT ON COLUMN "chat_room_members"."member_role" IS 'THÊM MỚI. REQUESTER (người cần hỗ trợ) | AGENT (Admin xử lý) | MEMBER';

COMMENT ON COLUMN "chat_room_members"."last_read_seq" IS 'THÊM MỚI. Đã đọc tới tin số mấy; số tin chưa đọc = last_message_seq - last_read_seq';

COMMENT ON COLUMN "chat_room_members"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "chat_messages" IS 'Tin nhắn chat. Giữ nguyên các cột của sale-app (room_id, sender_id, sender_username, body), thêm seq, loại tin và cờ ẩn. Không sửa nội dung sau khi gửi.';

COMMENT ON COLUMN "chat_messages"."sender_id" IS 'KHÁC sale-app: cho phép null vì tin hệ thống không có người gửi';

COMMENT ON COLUMN "chat_messages"."sender_username" IS 'Chép từ users.username (trùng email với tài khoản mới) để broadcast nhanh; tin hệ thống ghi SYSTEM';

COMMENT ON COLUMN "chat_messages"."body" IS 'Tối đa 2.000 ký tự, kiểm tra ở tầng ứng dụng';

COMMENT ON COLUMN "chat_messages"."seq" IS 'THÊM MỚI. Số thứ tự trong phòng; client dùng để tải bù tin khi mất kết nối';

COMMENT ON COLUMN "chat_messages"."message_type" IS 'THÊM MỚI. TEXT | INTERNAL_NOTE (chỉ Admin thấy) | SYSTEM';

COMMENT ON COLUMN "chat_messages"."is_hidden" IS 'THÊM MỚI. Admin ẩn tin vi phạm; bản gốc vẫn lưu (CHAT-BR-05)';

COMMENT ON COLUMN "chat_messages"."hidden_by" IS 'THÊM MỚI';

COMMENT ON COLUMN "chat_messages"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "chat_message_attachments" IS 'Tệp đính kèm của tin nhắn, tối đa 5 tệp một tin.';

COMMENT ON COLUMN "chat_message_attachments"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "chat_support_tickets" IS 'Phần nghiệp vụ hỗ trợ của một phòng SUPPORT: chủ đề, đơn hoặc sự kiện liên quan, trạng thái xử lý, Admin phụ trách, SLA, đánh giá. Tách riêng để 3 bảng chat lõi giữ đúng cấu trúc sale-app.';

COMMENT ON COLUMN "chat_support_tickets"."room_id" IS 'Quan hệ 1-1 với phòng SUPPORT';

COMMENT ON COLUMN "chat_support_tickets"."requester_id" IS 'Khách hàng hoặc ban tổ chức mở hội thoại';

COMMENT ON COLUMN "chat_support_tickets"."topic" IS 'Mã trong system_configs nhóm CHAT_TOPIC: PAYMENT_TICKET, ACCOUNT, EVENT, FEEDBACK, OTHER. Hai mã PAYMENT_TICKET và EVENT có quy tắc riêng (bắt buộc gắn đơn/sự kiện), không được xóa';

COMMENT ON COLUMN "chat_support_tickets"."order_id" IS 'Bắt buộc khi topic = PAYMENT_TICKET';

COMMENT ON COLUMN "chat_support_tickets"."event_id" IS 'Bắt buộc khi topic = EVENT';

COMMENT ON COLUMN "chat_support_tickets"."status" IS 'OPEN | ASSIGNED | WAITING_USER | CLOSED';

COMMENT ON COLUMN "chat_support_tickets"."flow_id" IS 'Quy trình áp dụng (SUPPORT_TICKET); status phải là code của một bước trong quy trình này';

COMMENT ON COLUMN "chat_support_tickets"."current_step_id" IS 'Bước hiện tại trong quy trình (giống opportunities.current_step_id). Khóa ngoại ghép (flow_id, current_step_id, status) bảo đảm bước thuộc đúng quy trình và trùng status';

COMMENT ON COLUMN "chat_support_tickets"."assigned_admin_id" IS 'Admin đang xử lý';

COMMENT ON COLUMN "chat_support_tickets"."first_response_at" IS 'Lúc Admin trả lời lần đầu, để tính SLA 5 phút';

COMMENT ON COLUMN "chat_support_tickets"."closed_by" IS 'Email người đóng hoặc SYSTEM (tự đóng sau 48 giờ)';

COMMENT ON COLUMN "chat_support_tickets"."rating" IS '1-5 sao';

COMMENT ON COLUMN "chat_support_tickets"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "canned_responses" IS 'Câu trả lời mẫu của Admin trong chat.';

COMMENT ON COLUMN "canned_responses"."shortcut" IS 'Gõ /shortcut để chèn nhanh';

COMMENT ON COLUMN "canned_responses"."content" IS 'Dùng được biến {ten_khach}, {ma_don}';

COMMENT ON COLUMN "canned_responses"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "audit_logs" IS 'Nhật ký kiểm toán nghiệp vụ. CHỈ THÊM, không sửa, không xóa, lưu tối thiểu 2 năm (ADM-BR-01).';

COMMENT ON COLUMN "audit_logs"."actor_id" IS 'null khi là SYSTEM';

COMMENT ON COLUMN "audit_logs"."actor_name" IS 'Email hoặc SYSTEM';

COMMENT ON COLUMN "audit_logs"."action" IS 'EVENT_APPROVED | EVENT_REJECTED | EVENT_CANCELLED | ORDER_REFUNDED | USER_LOCKED | DOCUMENT_DELETED | CONFIG_CHANGED ...';

COMMENT ON COLUMN "audit_logs"."entity_type" IS 'EVENT | ORDER | USER | DOCUMENT | CONFIG | CHAT_ROOM';

COMMENT ON COLUMN "audit_logs"."before_data" IS 'Giá trị các trường trước khi đổi';

COMMENT ON COLUMN "audit_logs"."after_data" IS 'Giá trị sau khi đổi';

COMMENT ON COLUMN "audit_logs"."reason" IS 'Bắt buộc với thao tác nhạy cảm (PER-BR-06)';

COMMENT ON TABLE "alerts" IS 'Cảnh báo vận hành gửi tới Admin: lệch tồn kho, lệch số tiền, hoàn tiền thất bại, tệp có mã độc...';

COMMENT ON COLUMN "alerts"."code" IS 'ALR-01 ... ALR-09 theo BRD 08 mục 5';

COMMENT ON COLUMN "alerts"."severity" IS 'Mã trong system_configs nhóm ALERT_SEVERITY: CRITICAL, HIGH, MEDIUM, LOW';

COMMENT ON COLUMN "alerts"."status" IS 'OPEN | ACKNOWLEDGED | RESOLVED';

COMMENT ON COLUMN "alerts"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "inventory_reconciliations" IS 'Kết quả đối soát tồn kho Redis với PostgreSQL, chạy mỗi phút trong đợt bán (RSV-BR-08). Chỉ thêm. Quy tắc: PostgreSQL (ticket_types) luôn thắng. diff > 0 (Redis còn nhiều hơn DB, nguy cơ bán quá) -> tạm dừng bán loại vé đó, dựng lại Redis từ DB, ghi SALE_PAUSED. diff < 0 (Redis còn ít hơn DB, vé bị kẹt) -> dựng lại Redis từ DB, ghi REDIS_REBUILT. diff = 0 -> NONE.';

COMMENT ON COLUMN "inventory_reconciliations"."redis_available" IS 'Số vé còn theo Redis';

COMMENT ON COLUMN "inventory_reconciliations"."db_available" IS 'total - held - sold theo PostgreSQL';

COMMENT ON COLUMN "inventory_reconciliations"."diff" IS 'redis_available - db_available; khác 0 là lệch';

COMMENT ON COLUMN "inventory_reconciliations"."action_taken" IS 'NONE | SALE_PAUSED | REDIS_REBUILT';

COMMENT ON TABLE "export_jobs" IS 'Yêu cầu xuất danh sách đơn ra CSV, chạy nền (ADM-BR-05).';

COMMENT ON COLUMN "export_jobs"."export_type" IS 'ORDERS_CSV';

COMMENT ON COLUMN "export_jobs"."status" IS 'QUEUED | RUNNING | DONE | FAILED';

COMMENT ON COLUMN "export_jobs"."document_id" IS 'File kết quả, link tải hiệu lực 24 giờ';

COMMENT ON COLUMN "export_jobs"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "integration_outbox" IS 'Transactional outbox (TECH-K-03), theo mẫu integration_outbox của sale-app: ghi cùng transaction với dữ liệu nghiệp vụ, tiến trình relay đọc dòng PENDING rồi phát lên Kafka. Index cho relay là index có điều kiện ở mục 6. Chỉ thêm.';

COMMENT ON COLUMN "integration_outbox"."id" IS 'Cũng là eventId của message Kafka';

COMMENT ON COLUMN "integration_outbox"."aggregate_type" IS 'ORDER | REFUND | EVENT';

COMMENT ON COLUMN "integration_outbox"."topic" IS 'order.events | refund.events | event.events ...';

COMMENT ON COLUMN "integration_outbox"."message_key" IS 'Key Kafka để giữ thứ tự theo đối tượng';

COMMENT ON COLUMN "integration_outbox"."event_type" IS 'order.created | order.paid | refund.requested ...';

COMMENT ON COLUMN "integration_outbox"."status" IS 'PENDING | SENT | FAILED';

COMMENT ON TABLE "processed_events" IS 'Các message Kafka đã xử lý, để consumer bỏ qua khi nhận trùng (TECH-K-02).';

COMMENT ON TABLE "mail_templates" IS 'Mẫu email theo mã, theo mẫu mail_template của sale-app. Sửa nội dung mail không phải sửa code.';

COMMENT ON COLUMN "mail_templates"."code" IS 'ORDER_TICKETS, REFUND_COMPLETED, EVENT_CANCELLED, EVENT_REVIEWED, CHAT_NEW_MESSAGE, ROLE_REQUEST_REVIEWED';

COMMENT ON COLUMN "mail_templates"."subject_template" IS 'Tiêu đề, dùng biến {{orderCode}}, {{eventName}}...';

COMMENT ON COLUMN "mail_templates"."body_text" IS 'Bản chữ thuần cho trình đọc mail không hiển thị HTML';

COMMENT ON COLUMN "mail_templates"."is_deleted" IS 'Xóa mềm';

COMMENT ON TABLE "mail_queue" IS 'Hàng đợi email, theo mẫu mail_queue của sale-app: PENDING -> SENT / FAILED. Thay cho bảng email_logs. Index cho job gửi là index có điều kiện ở mục 6.';

COMMENT ON COLUMN "mail_queue"."user_id" IS 'Người nhận nếu là user trong hệ thống';

COMMENT ON COLUMN "mail_queue"."variables" IS 'Giá trị điền vào mẫu, ví dụ {"orderCode": "FS-000002"}';

COMMENT ON COLUMN "mail_queue"."ref_type" IS 'ORDER | EVENT | CHAT_ROOM | ROLE_REQUEST';

COMMENT ON COLUMN "mail_queue"."ref_id" IS 'Id đối tượng dạng chuỗi (uuid hoặc số, vì role_requests.id là bigint)';

COMMENT ON COLUMN "mail_queue"."status" IS 'PENDING | SENT | FAILED';

COMMENT ON COLUMN "mail_queue"."idempotency_key" IS 'Chống gửi trùng khi message Kafka đến lặp, ví dụ ORDER_TICKETS:<orderId>:1';

COMMENT ON COLUMN "mail_queue"."is_deleted" IS 'Xóa mềm';

ALTER TABLE "events" ADD FOREIGN KEY ("organizer_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "events" ADD FOREIGN KEY ("province_id") REFERENCES "provinces" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "events" ADD FOREIGN KEY ("flow_id") REFERENCES "flows" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "events" ADD FOREIGN KEY ("reviewed_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "events" ADD FOREIGN KEY ("cancelled_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "event_documents" ADD FOREIGN KEY ("event_id") REFERENCES "events" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "ticket_types" ADD FOREIGN KEY ("event_id") REFERENCES "events" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "reservations" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "reservations" ADD FOREIGN KEY ("event_id") REFERENCES "events" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "reservations" ADD FOREIGN KEY ("ticket_type_id") REFERENCES "ticket_types" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "orders" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "orders" ADD FOREIGN KEY ("event_id") REFERENCES "events" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "orders" ADD FOREIGN KEY ("reservation_id") REFERENCES "reservations" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "orders" ADD FOREIGN KEY ("ticket_type_id") REFERENCES "ticket_types" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "orders" ADD FOREIGN KEY ("flow_id") REFERENCES "flows" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "payment_transactions" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "tickets" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "tickets" ADD FOREIGN KEY ("event_id") REFERENCES "events" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "tickets" ADD FOREIGN KEY ("ticket_type_id") REFERENCES "ticket_types" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "tickets" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "refunds" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "refunds" ADD FOREIGN KEY ("flow_id") REFERENCES "flows" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "refunds" ADD FOREIGN KEY ("requested_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "flow_steps" ADD FOREIGN KEY ("flow_id") REFERENCES "flows" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "event_flows" ADD FOREIGN KEY ("event_id") REFERENCES "events" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "event_flows" ADD FOREIGN KEY ("from_step_id") REFERENCES "flow_steps" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "event_flows" ADD FOREIGN KEY ("flow_step_id") REFERENCES "flow_steps" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "event_flows" ADD FOREIGN KEY ("processed_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "order_flows" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "order_flows" ADD FOREIGN KEY ("from_step_id") REFERENCES "flow_steps" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "order_flows" ADD FOREIGN KEY ("flow_step_id") REFERENCES "flow_steps" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "order_flows" ADD FOREIGN KEY ("processed_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "refund_flows" ADD FOREIGN KEY ("refund_id") REFERENCES "refunds" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "refund_flows" ADD FOREIGN KEY ("from_step_id") REFERENCES "flow_steps" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "refund_flows" ADD FOREIGN KEY ("flow_step_id") REFERENCES "flow_steps" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "refund_flows" ADD FOREIGN KEY ("processed_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "support_ticket_flows" ADD FOREIGN KEY ("room_id") REFERENCES "chat_support_tickets" ("room_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "support_ticket_flows" ADD FOREIGN KEY ("from_step_id") REFERENCES "flow_steps" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "support_ticket_flows" ADD FOREIGN KEY ("flow_step_id") REFERENCES "flow_steps" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "support_ticket_flows" ADD FOREIGN KEY ("processed_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "documents" ADD FOREIGN KEY ("document_type_code") REFERENCES "document_types" ("code") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "documents" ADD FOREIGN KEY ("current_version_id") REFERENCES "document_versions" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "documents" ADD FOREIGN KEY ("owner_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "document_versions" ADD FOREIGN KEY ("document_id") REFERENCES "documents" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "role_request_documents" ADD FOREIGN KEY ("request_id") REFERENCES "role_requests" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_room_members" ADD FOREIGN KEY ("room_id") REFERENCES "chat_rooms" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_room_members" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_messages" ADD FOREIGN KEY ("room_id") REFERENCES "chat_rooms" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_messages" ADD FOREIGN KEY ("sender_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_messages" ADD FOREIGN KEY ("hidden_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_message_attachments" ADD FOREIGN KEY ("message_id") REFERENCES "chat_messages" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_message_attachments" ADD FOREIGN KEY ("document_id") REFERENCES "documents" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_support_tickets" ADD FOREIGN KEY ("room_id") REFERENCES "chat_rooms" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_support_tickets" ADD FOREIGN KEY ("requester_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_support_tickets" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_support_tickets" ADD FOREIGN KEY ("event_id") REFERENCES "events" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_support_tickets" ADD FOREIGN KEY ("flow_id") REFERENCES "flows" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_support_tickets" ADD FOREIGN KEY ("assigned_admin_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "audit_logs" ADD FOREIGN KEY ("actor_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "alerts" ADD FOREIGN KEY ("acknowledged_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "inventory_reconciliations" ADD FOREIGN KEY ("ticket_type_id") REFERENCES "ticket_types" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "export_jobs" ADD FOREIGN KEY ("requested_by") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "export_jobs" ADD FOREIGN KEY ("event_id") REFERENCES "events" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "export_jobs" ADD FOREIGN KEY ("document_id") REFERENCES "documents" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "mail_queue" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "mail_queue" ADD FOREIGN KEY ("template_code") REFERENCES "mail_templates" ("code") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "reservations" ADD CONSTRAINT "fk_reservations_ticket_type_event" FOREIGN KEY ("ticket_type_id", "event_id") REFERENCES "ticket_types" ("id", "event_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "orders" ADD CONSTRAINT "fk_orders_reservation_match" FOREIGN KEY ("reservation_id", "user_id", "event_id", "ticket_type_id", "quantity") REFERENCES "reservations" ("id", "user_id", "event_id", "ticket_type_id", "quantity") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "tickets" ADD CONSTRAINT "fk_tickets_order_match" FOREIGN KEY ("order_id", "user_id", "event_id", "ticket_type_id") REFERENCES "orders" ("id", "user_id", "event_id", "ticket_type_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "payment_transactions" ADD CONSTRAINT "fk_payment_order_amount" FOREIGN KEY ("order_id", "amount") REFERENCES "orders" ("id", "total_amount") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_support_tickets" ADD CONSTRAINT "fk_support_order_owner" FOREIGN KEY ("order_id", "requester_id") REFERENCES "orders" ("id", "user_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "event_documents" ADD CONSTRAINT "fk_event_documents_document" FOREIGN KEY ("document_id", "document_type_code") REFERENCES "documents" ("id", "document_type_code") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "role_request_documents" ADD CONSTRAINT "fk_role_request_documents_document" FOREIGN KEY ("document_id", "document_type_code") REFERENCES "documents" ("id", "document_type_code") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "events" ADD CONSTRAINT "fk_events_current_step" FOREIGN KEY ("flow_id", "current_step_id", "status") REFERENCES "flow_steps" ("flow_id", "id", "code") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "orders" ADD CONSTRAINT "fk_orders_current_step" FOREIGN KEY ("flow_id", "current_step_id", "status") REFERENCES "flow_steps" ("flow_id", "id", "code") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "refunds" ADD CONSTRAINT "fk_refunds_current_step" FOREIGN KEY ("flow_id", "current_step_id", "status") REFERENCES "flow_steps" ("flow_id", "id", "code") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "chat_support_tickets" ADD CONSTRAINT "fk_chat_support_tickets_current_step" FOREIGN KEY ("flow_id", "current_step_id", "status") REFERENCES "flow_steps" ("flow_id", "id", "code") DEFERRABLE INITIALLY IMMEDIATE;
