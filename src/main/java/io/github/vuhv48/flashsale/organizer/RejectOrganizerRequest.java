package io.github.vuhv48.flashsale.organizer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Lý do từ chối yêu cầu vai trò")
public record RejectOrganizerRequest(
		@Schema(description = "Lý do, từ 10 đến 500 ký tự", example = "Thiếu giấy tờ đăng ký kinh doanh")
		@NotBlank @Size(min = 10, max = 500) String reason
) {
	public RejectOrganizerRequest {
		if (reason != null) {
			reason = reason.trim();
		}
	}
}
