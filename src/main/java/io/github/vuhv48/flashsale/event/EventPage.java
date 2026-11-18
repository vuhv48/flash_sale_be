package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record EventPage<T>(
		List<T> items,
		@Schema(example = "1") long total,
		@Schema(example = "0") int page,
		@Schema(example = "20") int size
) {
}
