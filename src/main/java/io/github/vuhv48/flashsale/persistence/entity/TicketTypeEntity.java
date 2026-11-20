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
@Table(name = "ticket_types")
public class TicketTypeEntity extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false)
	private long price;

	@Column(name = "total_quantity", nullable = false)
	private int totalQuantity;

	@Column(name = "held_quantity", nullable = false)
	private int heldQuantity;

	@Column(name = "sold_quantity", nullable = false)
	private int soldQuantity;

	@Column(name = "sale_paused", nullable = false)
	private boolean salePaused;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public int available() {
		return totalQuantity - heldQuantity - soldQuantity;
	}
}
