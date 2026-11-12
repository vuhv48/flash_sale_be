package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Đổi mật khẩu của tài khoản đang đăng nhập")
public record ChangePasswordRequest(
		@Schema(description = "Mật khẩu hiện tại", example = "password123")
		@NotBlank String currentPassword,
		@Schema(description = "Mật khẩu mới, 8–64 ký tự, có chữ hoa, chữ thường và số", example = "MatKhauMoi1")
		@NotBlank @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự, có chữ hoa, chữ thường và số") String newPassword
) {
}
