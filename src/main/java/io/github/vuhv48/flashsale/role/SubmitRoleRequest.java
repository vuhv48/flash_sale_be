package io.github.vuhv48.flashsale.role;

import io.github.vuhv48.flashsale.auth.OrganizerApplication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitRoleRequest(
		@NotBlank String roleCode,
		@NotBlank @Pattern(regexp = "ADD|REMOVE") String action,
		@Size(max = 500) String note,
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
