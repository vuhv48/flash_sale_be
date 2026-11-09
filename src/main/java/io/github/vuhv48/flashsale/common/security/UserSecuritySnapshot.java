package io.github.vuhv48.flashsale.common.security;

import java.util.Set;
import java.util.UUID;

public record UserSecuritySnapshot(
		UUID id,
		String username,
		String passwordHash,
		boolean enabled,
		String dataScope,
		Set<String> roleCodes,
		Set<String> effectivePermissionCodes
) {
}
