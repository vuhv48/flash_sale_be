package io.github.vuhv48.flashsale.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrganizerEventDetail(
		UUID id,
		String code,
		String name,
		String description,
		String category,
		String categoryLabel,
		UUID provinceId,
		String provinceName,
		String venueName,
		String venueAddress,
		Instant startsAt,
		Instant endsAt,
		Instant waitingRoomOpensAt,
		Instant saleStartsAt,
		Instant saleEndsAt,
		int maxTicketsPerUser,
		int holdMinutes,
		int admissionRate,
		String status,
		String statusLabel,
		Instant submittedAt,
		Instant reviewedAt,
		String rejectReason,
		int pauseCount,
		Instant pausedAt,
		String cancelReason,
		UUID organizerId,
		List<OrganizerTicketType> ticketTypes,
		List<EventImageView> images
) {
}
