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
@Table(name = "events")
public class EventEntity extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 20)
	private String code;

	@Column(name = "organizer_id", nullable = false)
	private UUID organizerId;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(length = 50)
	private String category;

	@Column(name = "venue_name", length = 200)
	private String venueName;

	@Column(name = "venue_address", length = 300)
	private String venueAddress;

	@Column(name = "province_id")
	private UUID provinceId;

	@Column(name = "starts_at")
	private Instant startsAt;

	@Column(name = "ends_at")
	private Instant endsAt;

	@Column(name = "waiting_room_opens_at")
	private Instant waitingRoomOpensAt;

	@Column(name = "sale_starts_at")
	private Instant saleStartsAt;

	@Column(name = "sale_ends_at")
	private Instant saleEndsAt;

	@Column(name = "max_tickets_per_user", nullable = false)
	private int maxTicketsPerUser = 4;

	@Column(name = "hold_minutes", nullable = false)
	private int holdMinutes = 10;

	@Column(name = "admission_rate", nullable = false)
	private int admissionRate = 200;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "flow_id", nullable = false)
	private UUID flowId;

	@Column(name = "current_step_id", nullable = false)
	private UUID currentStepId;

	@Column(name = "submitted_at")
	private Instant submittedAt;

	@Column(name = "reviewed_by")
	private UUID reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "reject_reason", length = 500)
	private String rejectReason;

	@Column(name = "pause_count", nullable = false)
	private int pauseCount;

	@Column(name = "paused_at")
	private Instant pausedAt;

	@Column(name = "cancelled_by")
	private UUID cancelledBy;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "cancel_reason", length = 500)
	private String cancelReason;
}
