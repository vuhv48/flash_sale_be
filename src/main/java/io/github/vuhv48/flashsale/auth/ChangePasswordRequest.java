package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Đổi mật khẩu của tài khoản đang đăng nhập")
public record ChangePasswordRequest(
		@Schema(description = "Mật khẩu hiện tại", example = "password123")
		@NotBlank String currentPassword,
		@Schema(description = "Mật khẩu mới, 8–128 ký tự", example = "password456")
		@NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
