package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cặp token sau khi đăng nhập hoặc làm mới phiên")
public record LoginResponse(
		@Schema(description = "JWT gửi kèm header Authorization", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ2dWhvYW5nIn0.demo")
		String accessToken,
		@Schema(description = "Refresh token, chỉ gửi lên API làm mới phiên", example = "dGhpcy1pcy1hLWRlbW8tcmVmcmVzaC10b2tlbg")
		String refreshToken,
		@Schema(description = "Loại token", example = "Bearer")
		String tokenType,
		@Schema(description = "Số giây access token còn hạn", example = "86400")
		long expiresInSeconds,
		@Schema(description = "Số giây refresh token còn hạn", example = "604800")
		long refreshExpiresInSeconds
) {
}
