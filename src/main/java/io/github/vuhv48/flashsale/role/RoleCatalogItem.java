package io.github.vuhv48.flashsale.role;

public record RoleCatalogItem(
		String code,
		String name,
		String description,
		boolean requiresOrgProfile
) {
}
