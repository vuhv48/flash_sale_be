package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record MessageResponse(
		@Schema(description = "Kết quả xử lý", example = "Đăng xuất thành công")
		String message
) {
}
