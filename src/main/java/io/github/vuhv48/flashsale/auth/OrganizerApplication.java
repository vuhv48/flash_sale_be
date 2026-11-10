package io.github.vuhv48.flashsale.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrganizerApplication(
		@NotBlank @Size(max = 200) String orgName,
		@NotBlank
		@Pattern(regexp = "^[0-9]{10}$|^[0-9]{13}$", message = "Mã số thuế phải gồm 10 hoặc 13 chữ số")
		String taxCode,
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
