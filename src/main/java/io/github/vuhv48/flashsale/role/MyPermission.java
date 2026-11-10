package io.github.vuhv48.flashsale.role;

public record MyPermission(
		String code,
		String name,
		String group,
		String action,
		String accessLevel
) {
}
