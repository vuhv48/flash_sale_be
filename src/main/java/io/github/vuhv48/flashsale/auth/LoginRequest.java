package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Thông tin đăng nhập")
public record LoginRequest(
		@Schema(description = "Tên đăng nhập", example = "vuhoang")
		@NotBlank String username,
		@Schema(description = "Mật khẩu", example = "password123")
		@NotBlank String password
) {
}
