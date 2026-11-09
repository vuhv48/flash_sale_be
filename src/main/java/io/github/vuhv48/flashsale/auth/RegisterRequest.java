package io.github.vuhv48.flashsale.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(min = 3, max = 64) String username,
		@NotBlank @Size(min = 8, max = 128) String password,
		@Size(max = 320)
		@Pattern(
				regexp = "^$|^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$",
				message = "Email không đúng định dạng"
		)
		String email
) {
	public RegisterRequest {
		if (email != null) {
			email = email.trim();
		}
	}
}
