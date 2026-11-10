package io.github.vuhv48.flashsale.organizer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectOrganizerRequest(
		@NotBlank @Size(min = 10, max = 500) String reason
) {
	public RejectOrganizerRequest {
		if (reason != null) {
			reason = reason.trim();
		}
	}
}
