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
@Table(name = "system_configs")
public class SystemConfigEntity extends BaseAuditableEntity {

	@Id
	private UUID id;

	@Column(name = "group_code", nullable = false, length = 50)
	private String groupCode;

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 200)
	private String label;

	@Column(length = 500)
	private String value;

	@Column(name = "order_no", nullable = false)
	private int orderNo;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;
}
