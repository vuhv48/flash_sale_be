package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token nhận được lúc đăng ký hoặc đăng nhập")
public record RefreshTokenRequest(
		@Schema(description = "Refresh token còn hạn", example = "dGhpcy1pcy1hLWRlbW8tcmVmcmVzaC10b2tlbg")
		@NotBlank String refreshToken
) {
}
