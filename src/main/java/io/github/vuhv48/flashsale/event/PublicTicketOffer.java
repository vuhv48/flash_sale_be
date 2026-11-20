package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record PublicTicketOffer(
		@Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
		@Schema(example = "VIP") String name,
		@Schema(example = "500000") long price,
		@Schema(description = "AVAILABLE, LOW hoặc SOLD_OUT", example = "AVAILABLE") String availability,
		@Schema(description = "Còn, Sắp hết hoặc Hết", example = "Còn") String availabilityLabel,
		@Schema(example = "1") int sortOrder
) {
}
