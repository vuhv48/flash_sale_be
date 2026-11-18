package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
		@Schema(description = "Mã trong email đặt lại mật khẩu", example = "dGhpcy1pcy1tYS1kYXQtbGFp")
		@NotBlank String token,
		@Schema(example = "MatKhauMoi1")
		@NotBlank @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự, có chữ hoa, chữ thường và số") String newPassword
) {
}
