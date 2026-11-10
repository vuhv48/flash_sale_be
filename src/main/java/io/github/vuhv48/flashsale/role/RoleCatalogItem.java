package io.github.vuhv48.flashsale.role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Một vai trò")
public record RoleCatalogItem(
		@Schema(description = "Mã vai trò", example = "ORGANIZER")
		String code,
		@Schema(description = "Tên vai trò", example = "Ban tổ chức")
		String name,
		@Schema(description = "Mô tả", example = "Xin thêm trong hồ sơ, kèm hồ sơ đơn vị, Admin duyệt")
		String description,
		@Schema(description = "Xin thêm vai trò này có phải gửi hồ sơ đơn vị không", example = "true")
		boolean requiresOrgProfile
) {
}
