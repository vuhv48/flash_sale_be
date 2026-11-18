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
@Table(name = "flow_steps")
public class FlowStepEntity extends BaseAuditableEntity {

	@Id
	private UUID id;

	@Column(name = "flow_id", nullable = false)
	private UUID flowId;

	@Column(nullable = false, length = 30)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "step_order", nullable = false)
	private int stepOrder;

	@Column(name = "is_initial", nullable = false)
	private boolean initial;

	@Column(name = "is_final", nullable = false)
	private boolean finalStep;
}
