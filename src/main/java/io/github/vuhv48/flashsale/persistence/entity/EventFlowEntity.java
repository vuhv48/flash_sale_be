package io.github.vuhv48.flashsale.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "event_flows")
public class EventFlowEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(name = "step_no", nullable = false)
	private int stepNo;

	@Column(name = "from_step_id")
	private UUID fromStepId;

	@Column(name = "flow_step_id", nullable = false)
	private UUID flowStepId;

	@Column(nullable = false, length = 50)
	private String action;

	@Column(name = "processed_by")
	private UUID processedBy;

	@Column(name = "processed_by_name", nullable = false, length = 255)
	private String processedByName;

	@Column(length = 500)
	private String reason;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;
}
