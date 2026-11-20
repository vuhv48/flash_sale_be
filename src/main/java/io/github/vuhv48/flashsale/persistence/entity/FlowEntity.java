package io.github.vuhv48.flashsale.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "flows")
public class FlowEntity extends BaseAuditableEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "entity_type", nullable = false, length = 30)
	private String entityType;

	@Column(nullable = false)
	private boolean enabled = true;
}
