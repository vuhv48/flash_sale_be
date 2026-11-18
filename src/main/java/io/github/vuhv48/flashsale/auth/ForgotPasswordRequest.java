package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
		@Schema(example = "vu@example.com")
		@NotBlank @Email @Size(max = 254) String email
) {
	public ForgotPasswordRequest {
		if (email != null) {
			email = email.trim().toLowerCase();
		}
	}
}
