package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record OrganizerTicketType(
		UUID id,
		@Schema(example = "VIP") String name,
		@Schema(example = "500000") long price,
		@Schema(example = "1000") int totalQuantity,
		@Schema(example = "0") int heldQuantity,
		@Schema(example = "0") int soldQuantity,
		@Schema(example = "1000") int available,
		@Schema(example = "1") int sortOrder,
		boolean salePaused
) {
}
