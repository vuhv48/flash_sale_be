package io.github.vuhv48.flashsale.role;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Một yêu cầu thêm hoặc gỡ vai trò")
public record RoleRequestView(
		@Schema(description = "Mã yêu cầu", example = "12")
		Long id,
		@Schema(description = "Người gửi yêu cầu", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
		UUID userId,
		@Schema(description = "Tên đăng nhập của người gửi", example = "user")
		String username,
		@Schema(description = "Mã vai trò", example = "ORGANIZER")
		String roleCode,
		@Schema(description = "Tên vai trò", example = "Ban tổ chức")
		String roleName,
		@Schema(description = "ADD là xin thêm, REMOVE là xin gỡ", example = "ADD")
		String action,
		@Schema(description = "PENDING, APPROVED, REJECTED hoặc CANCELLED", example = "PENDING")
		String status,
		@Schema(description = "Ghi chú của người gửi", example = "Công ty tổ chức concert")
		String note,
		@Schema(description = "Lý do từ chối. Chỉ có khi trạng thái là REJECTED")
		String rejectReason,
		@Schema(description = "true khi Admin không duyệt được lúc này", example = "false")
		boolean approveBlocked,
		@Schema(description = "Lý do không duyệt được. Có khi approveBlocked là true")
		String approveBlockReason,
		@Schema(description = "Thời điểm gửi yêu cầu", example = "2026-11-10T07:50:12Z")
		Instant createdAt,
		@Schema(description = "Thời điểm Admin duyệt hoặc từ chối. Trống khi còn PENDING hoặc đã hủy")
		Instant reviewedAt
) {
}
