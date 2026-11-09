package io.github.vuhv48.flashsale.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityPermitProperties(List<PermitPath> permitAll) {

	public SecurityPermitProperties {
		permitAll = permitAll != null ? List.copyOf(permitAll) : List.of();
	}

	public record PermitPath(String method, String path) {
	}
}
