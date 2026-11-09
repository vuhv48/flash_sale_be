package io.github.vuhv48.flashsale.auth;

public record LoginResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresInSeconds,
		long refreshExpiresInSeconds
) {
}
