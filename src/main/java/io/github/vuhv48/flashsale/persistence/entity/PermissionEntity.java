package io.github.vuhv48.flashsale.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "permissions", schema = "authz")
public class PermissionEntity extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 128)
	private String code;

	@Column(name = "permission_group", nullable = false, length = 64)
	private String permissionGroup;

	@Column(name = "action_code", nullable = false, length = 32)
	private String actionCode;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false)
	private boolean enabled = true;
}
