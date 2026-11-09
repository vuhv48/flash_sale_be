package io.github.vuhv48.flashsale.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
		String issuer,
		long expirationSeconds,
		long refreshExpirationSeconds
) {
}
