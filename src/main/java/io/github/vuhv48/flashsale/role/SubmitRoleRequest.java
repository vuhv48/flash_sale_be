package io.github.vuhv48.flashsale.role;

import io.github.vuhv48.flashsale.auth.OrganizerApplication;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu thêm hoặc gỡ một vai trò. ADMIN không xin được.")
public record SubmitRoleRequest(
		@Schema(description = "Mã vai trò được phép xin", example = "ORGANIZER")
		@NotBlank String roleCode,
		@Schema(description = "ADD là xin thêm, REMOVE là xin gỡ", example = "ADD")
		@NotBlank @Pattern(regexp = "ADD|REMOVE") String action,
		@Schema(description = "Ghi chú, không bắt buộc", example = "Công ty tổ chức concert")
		@Size(max = 500) String note,
		@Schema(description = "Hồ sơ đơn vị. Bắt buộc khi xin thêm vai trò yêu cầu hồ sơ")
		@Valid OrganizerApplication organizer
) {
	public SubmitRoleRequest {
		if (roleCode != null) {
			roleCode = roleCode.trim();
		}
		if (action != null) {
			action = action.trim();
		}
		if (note != null) {
			note = note.trim();
		}
	}
}
