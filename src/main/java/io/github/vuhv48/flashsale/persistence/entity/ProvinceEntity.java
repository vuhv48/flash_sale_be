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
@Table(name = "provinces")
public class ProvinceEntity extends BaseAuditableEntity {

	@Id
	private UUID id;

	@Column(name = "gso_code", nullable = false, length = 2)
	private String gsoCode;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "short_name", nullable = false, length = 100)
	private String shortName;

	@Column(nullable = false, length = 20)
	private String type;

	@Column(name = "order_no", nullable = false)
	private int orderNo;
}
