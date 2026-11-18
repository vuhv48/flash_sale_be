package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record PublicEventCard(
		UUID id,
		@Schema(example = "EV-000001") String code,
		@Schema(example = "Đêm nhạc Mùa Đông 2026") String name,
		@Schema(example = "MUSIC") String category,
		@Schema(example = "Âm nhạc") String categoryLabel,
		@Schema(example = "Thành phố Hà Nội") String provinceName,
		@Schema(example = "Sân vận động Mỹ Đình") String venueName,
		Instant startsAt,
		Instant saleStartsAt,
		Instant saleEndsAt,
		@Schema(example = "ON_SALE") String status,
		@Schema(example = "Đang bán") String statusLabel,
		UUID coverDocumentId,
		@Schema(description = "Giá vé thấp nhất", example = "250000") Long minPrice
) {
}
