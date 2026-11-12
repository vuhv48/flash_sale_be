package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Đăng nhập bằng email và mật khẩu")
public record LoginRequest(
		@Schema(example = "vu@example.com")
		@NotBlank @Email @Size(max = 254) String email,
		@Schema(example = "MatKhau123")
		@NotBlank String password
) {
	public LoginRequest {
		if (email != null) {
			email = email.trim().toLowerCase();
		}
	}
}
