package io.github.vuhv48.flashsale.role;

import java.time.Instant;
import java.util.UUID;

public record RoleRequestView(
		Long id,
		UUID userId,
		String username,
		String roleCode,
		String roleName,
		String action,
		String status,
		String note,
		String rejectReason,
		boolean approveBlocked,
		String approveBlockReason,
		Instant createdAt,
		Instant reviewedAt
) {
}
