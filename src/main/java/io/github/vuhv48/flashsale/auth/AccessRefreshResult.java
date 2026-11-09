package io.github.vuhv48.flashsale.auth;

public record AccessRefreshResult(
		String accessToken,
		String refreshToken,
		long accessExpiresInSeconds,
		long refreshExpiresInSeconds
) {
}
