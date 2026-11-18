package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Hồ sơ của tài khoản đang đăng nhập. Email không đổi sau khi đăng ký.")
public record ProfileResponse(
		@Schema(example = "vu@example.com") String email,
		@Schema(example = "Vũ Hoàng") String fullName,
		@Schema(example = "0912345678") String phone,
		@Schema(description = "false khi Admin đã khóa tài khoản", example = "true") boolean enabled
) {
}
