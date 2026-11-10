package io.github.vuhv48.flashsale.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrantRoleRequest(
		@NotBlank String roleCode,
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
