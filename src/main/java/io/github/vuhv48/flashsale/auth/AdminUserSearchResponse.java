package io.github.vuhv48.flashsale.auth;

import java.util.UUID;

public record AdminUserSearchResponse(
		UUID id,
		String username,
		boolean enabled,
		String dataScope
) {
}
