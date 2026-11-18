package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record EventImageView(
		UUID id,
		@Schema(example = "EVENT_COVER") String type,
		UUID documentId,
		@Schema(example = "cover.jpg") String fileName,
		@Schema(example = "image/jpeg") String mimeType,
		@Schema(example = "0") int sortOrder
) {
}
