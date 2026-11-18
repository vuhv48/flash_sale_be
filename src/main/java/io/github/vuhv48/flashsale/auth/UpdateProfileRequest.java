package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Sửa họ tên và số điện thoại. Không đổi email.")
public record UpdateProfileRequest(
		@Schema(example = "Vũ Hoàng")
		@NotBlank @Size(min = 2, max = 100) String fullName,
		@Schema(example = "0912345678")
		@NotBlank @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
		String phone
) {
	public UpdateProfileRequest {
		if (fullName != null) {
			fullName = fullName.trim();
		}
		if (phone != null) {
			phone = phone.trim();
		}
	}
}
