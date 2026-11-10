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
@Table(name = "organizer_profiles")
public class OrganizerProfileEntity extends BaseAuditableEntity {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "org_name", nullable = false, length = 200)
	private String orgName;

	@Column(name = "tax_code", nullable = false, length = 13)
	private String taxCode;

	@Column(nullable = false, length = 100)
	private String representative;
}
