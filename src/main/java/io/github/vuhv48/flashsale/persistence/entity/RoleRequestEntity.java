package io.github.vuhv48.flashsale.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "role_requests")
public class RoleRequestEntity extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "role_code", nullable = false, length = 64)
	private String roleCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private RoleRequestAction action;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoleRequestStatus status = RoleRequestStatus.PENDING;

	@Column(name = "user_note", length = 500)
	private String userNote;

	@Column(name = "reject_reason", length = 500)
	private String rejectReason;

	@Column(name = "reviewed_by")
	private UUID reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;
}
