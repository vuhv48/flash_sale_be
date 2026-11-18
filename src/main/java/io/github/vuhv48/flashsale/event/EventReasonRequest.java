package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Lý do từ chối hoặc hủy sự kiện")
public record EventReasonRequest(
		@Schema(description = "Lý do, từ 10 đến 500 ký tự", example = "Thiếu địa chỉ và giờ diễn ra cụ thể")
		@NotBlank(message = "Cần nhập lý do")
		@Size(min = 10, max = 500, message = "Lý do phải từ 10 đến 500 ký tự")
		String reason
) {
	public EventReasonRequest {
		if (reason != null) {
			reason = reason.trim();
		}
	}
}
