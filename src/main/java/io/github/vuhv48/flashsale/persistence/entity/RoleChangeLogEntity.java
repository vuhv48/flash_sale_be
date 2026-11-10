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
@Table(name = "role_change_logs", schema = "authz")
public class RoleChangeLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "actor_user_id")
	private UUID actorUserId;

	@Column(name = "target_user_id", nullable = false)
	private UUID targetUserId;

	@Column(name = "role_code", nullable = false, length = 64)
	private String roleCode;

	@Column(nullable = false, length = 10)
	private String action;

	@Column(nullable = false, length = 20)
	private String source;

	@Column(name = "request_id")
	private Long requestId;

	@Column(length = 500)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();
}
