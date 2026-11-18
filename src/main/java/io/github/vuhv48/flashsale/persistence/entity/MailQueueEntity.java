package io.github.vuhv48.flashsale.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "mail_queue")
public class MailQueueEntity extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "to_address", nullable = false, length = 320)
	private String toAddress;

	@Column(name = "template_code", nullable = false, length = 64)
	private String templateCode;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String variables;

	@Column(name = "ref_type", length = 30)
	private String refType;

	@Column(name = "ref_id", length = 64)
	private String refId;

	@Column(nullable = false, length = 20)
	private String status = "PENDING";

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "idempotency_key", nullable = false, length = 128)
	private String idempotencyKey;

	@Column(name = "sent_at")
	private Instant sentAt;
}
