package io.github.vuhv48.flashsale.role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Một quyền hiệu lực của tài khoản đang đăng nhập")
public record MyPermission(
		@Schema(description = "Mã quyền", example = "PER-04")
		String code,
		@Schema(description = "Tên quyền", example = "Xem, sửa hồ sơ; đổi mật khẩu; làm mới token; đăng xuất")
		String name,
		@Schema(description = "Nhóm quyền", example = "ACCOUNT")
		String group,
		@Schema(description = "Mã hành động", example = "MANAGE_PROFILE")
		String action,
		@Schema(description = "ALL là toàn bộ, OWN là dữ liệu của mình, AUTO là tác vụ hệ thống", example = "OWN")
		String accessLevel
) {
}
