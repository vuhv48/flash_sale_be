package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Đăng ký bằng email. Tài khoản mới luôn nhận vai trò CUSTOMER.")
public record RegisterRequest(
		@Schema(example = "vu@example.com")
		@NotBlank @Email @Size(max = 254) String email,
		@Schema(description = "8–64 ký tự, có chữ hoa, chữ thường và số, không trùng email", example = "MatKhau123")
		@NotBlank @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự, có chữ hoa, chữ thường và số") String password,
		@Schema(example = "MatKhau123")
		@NotBlank String confirmPassword,
		@Schema(example = "Vũ Hoàng")
		@NotBlank @Size(min = 2, max = 100) String fullName,
		@Schema(example = "0912345678")
		@NotBlank @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
		String phone,
		@Schema(description = "Phải tích đồng ý điều khoản", example = "true")
		@AssertTrue(message = "Bạn cần đồng ý điều khoản") boolean acceptedTerms
) {
	public RegisterRequest {
		if (email != null) {
			email = email.trim().toLowerCase();
		}
		if (fullName != null) {
			fullName = fullName.trim();
		}
		if (phone != null) {
			phone = phone.trim();
		}
	}

	@AssertTrue(message = "Mật khẩu nhập lại không khớp")
	public boolean isPasswordConfirmed() {
		return password != null && password.equals(confirmPassword);
	}
}
