package io.github.vuhv48.flashsale.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Hồ sơ đơn vị, bắt buộc khi xin thêm vai trò có requiresOrgProfile")
public record OrganizerApplication(
		@Schema(description = "Tên đơn vị", example = "Công ty Sự kiện ABC")
		@NotBlank @Size(max = 200) String orgName,
		@Schema(description = "Mã số thuế, 10 hoặc 13 chữ số", example = "0101234567")
		@NotBlank
		@Pattern(regexp = "^[0-9]{10}$|^[0-9]{13}$", message = "Mã số thuế phải gồm 10 hoặc 13 chữ số")
		String taxCode,
		@Schema(description = "Người đại diện", example = "Vũ Hoàng")
		@NotBlank @Size(max = 100) String representative
) {
	public OrganizerApplication {
		if (orgName != null) {
			orgName = orgName.trim();
		}
		if (taxCode != null) {
			taxCode = taxCode.trim();
		}
		if (representative != null) {
			representative = representative.trim();
		}
	}
}
