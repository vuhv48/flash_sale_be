package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountActionRequest(
		@Schema(description = "Lý do khóa hoặc mở khóa", example = "Vi phạm điều khoản sử dụng nhiều lần")
		@NotBlank @Size(max = 500) String reason
) {
	public AccountActionRequest {
		if (reason != null) {
			reason = reason.trim();
		}
	}
}
