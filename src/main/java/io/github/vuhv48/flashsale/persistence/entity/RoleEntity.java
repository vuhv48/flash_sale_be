package io.github.vuhv48.flashsale.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "roles", schema = "authz")
public class RoleEntity extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 64)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private boolean requestable = false;

	@Column(name = "requires_org_profile", nullable = false)
	private boolean requiresOrgProfile = false;

	@Column(nullable = false)
	private boolean enabled = true;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "role_permissions",
			schema = "authz",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "permission_id")
	)
	private Set<PermissionEntity> permissions = new HashSet<>();
}
