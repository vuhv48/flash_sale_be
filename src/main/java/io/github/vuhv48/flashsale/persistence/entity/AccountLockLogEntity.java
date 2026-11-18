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
@Table(name = "account_lock_logs", schema = "authz")
public class AccountLockLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "actor_user_id")
	private UUID actorUserId;

	@Column(nullable = false, length = 10)
	private String action;

	@Column(nullable = false, length = 500)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();
}
