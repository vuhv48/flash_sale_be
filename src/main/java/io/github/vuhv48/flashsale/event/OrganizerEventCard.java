package io.github.vuhv48.flashsale.event;

import java.time.Instant;
import java.util.UUID;

public record OrganizerEventCard(
		UUID id,
		String code,
		String name,
		String category,
		String categoryLabel,
		String provinceName,
		String status,
		String statusLabel,
		Instant saleStartsAt,
		Instant startsAt,
		Instant submittedAt,
		String rejectReason,
		int pauseCount,
		int ticketCount,
		UUID organizerId
) {
}
