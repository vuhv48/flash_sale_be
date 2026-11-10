package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kết quả đăng ký kèm cặp token")
public record RegisterResponse(
		@Schema(description = "Thông báo", example = "Đăng ký thành công.")
		String message,
		@Schema(description = "JWT gửi kèm header Authorization", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ2dWhvYW5nIn0.demo")
		String accessToken,
		@Schema(description = "Refresh token", example = "dGhpcy1pcy1hLWRlbW8tcmVmcmVzaC10b2tlbg")
		String refreshToken,
		@Schema(description = "Loại token", example = "Bearer")
		String tokenType,
		@Schema(description = "Số giây access token còn hạn", example = "86400")
		long expiresInSeconds,
		@Schema(description = "Số giây refresh token còn hạn", example = "604800")
		long refreshExpiresInSeconds
) {
}
