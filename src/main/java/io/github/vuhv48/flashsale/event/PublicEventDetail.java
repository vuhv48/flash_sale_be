package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicEventDetail(
		UUID id,
		@Schema(example = "EV-000001") String code,
		@Schema(example = "Đêm nhạc Mùa Đông 2026") String name,
		String description,
		@Schema(example = "MUSIC") String category,
		@Schema(example = "Âm nhạc") String categoryLabel,
		String provinceName,
		String venueName,
		String venueAddress,
		Instant startsAt,
		Instant endsAt,
		Instant waitingRoomOpensAt,
		Instant saleStartsAt,
		Instant saleEndsAt,
		@Schema(example = "ON_SALE") String status,
		@Schema(example = "Đang bán") String statusLabel,
		UUID coverDocumentId,
		List<PublicTicketOffer> ticketTypes,
		@Schema(example = "2026-12-19T01:00:00Z") Instant serverNow,
		@Schema(description = "SALE_START, SALE_END hoặc NONE", example = "SALE_END") String countdownTarget,
		@Schema(example = "3600") long countdownSeconds
) {
}
