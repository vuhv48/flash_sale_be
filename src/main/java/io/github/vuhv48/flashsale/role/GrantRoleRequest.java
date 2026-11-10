package io.github.vuhv48.flashsale.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin cấp thẳng một vai trò, kể cả ADMIN")
public record GrantRoleRequest(
		@Schema(description = "Mã vai trò", example = "ADMIN")
		@NotBlank String roleCode,
		@Schema(description = "Lý do, không bắt buộc", example = "Cấp quyền quản trị")
		@Size(max = 500) String reason
) {
	public GrantRoleRequest {
		if (roleCode != null) {
			roleCode = roleCode.trim();
		}
		if (reason != null) {
			reason = reason.trim();
		}
	}
}
