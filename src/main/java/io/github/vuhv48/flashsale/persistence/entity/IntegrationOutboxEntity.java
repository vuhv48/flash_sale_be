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
@Table(name = "integration_outbox")
public class IntegrationOutboxEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "aggregate_type", nullable = false, length = 30)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Column(nullable = false, length = 255)
	private String topic;

	@Column(name = "message_key", nullable = false, length = 255)
	private String messageKey;

	@Column(name = "event_type", nullable = false, length = 50)
	private String eventType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String payload;

	@Column(nullable = false, length = 32)
	private String status = "PENDING";

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
