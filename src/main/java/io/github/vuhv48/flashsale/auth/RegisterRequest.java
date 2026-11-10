package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Thông tin đăng ký. Tài khoản mới luôn nhận vai trò CUSTOMER.")
public record RegisterRequest(
		@Schema(description = "Tên đăng nhập, 3–64 ký tự", example = "vuhoang")
		@NotBlank @Size(min = 3, max = 64) String username,
		@Schema(description = "Mật khẩu, 8–128 ký tự", example = "password123")
		@NotBlank @Size(min = 8, max = 128) String password,
		@Schema(description = "Email, không bắt buộc. Nếu có thì đúng dạng ten@tenmien", example = "vu@example.com")
		@Size(max = 320)
		@Pattern(
				regexp = "^$|^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$",
				message = "Email không đúng định dạng"
		)
		String email,
		@Schema(description = "Họ tên, không bắt buộc", example = "Vũ Hoàng")
		@Size(max = 255) String fullName,
		@Schema(description = "Số điện thoại, không bắt buộc. 8–15 chữ số, có thể bắt đầu bằng +", example = "0912345678")
		@Size(max = 20)
		@Pattern(regexp = "^$|^\\+?[0-9]{8,15}$", message = "Số điện thoại không đúng định dạng")
		String phone
) {
	public RegisterRequest {
		if (email != null) {
			email = email.trim();
		}
		if (fullName != null) {
			fullName = fullName.trim();
		}
		if (phone != null) {
			phone = phone.trim();
		}
	}
}
