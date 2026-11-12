package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Một tài khoản trong danh sách Admin")
public record AdminUserSearchResponse(
		@Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
		@Schema(example = "vu@example.com") String email,
		@Schema(example = "Vũ Hoàng") String fullName,
		@Schema(example = "0912345678") String phone,
		@Schema(example = "true") boolean enabled
) {
}
