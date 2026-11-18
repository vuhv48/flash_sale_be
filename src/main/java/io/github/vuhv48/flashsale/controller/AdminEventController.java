package io.github.vuhv48.flashsale.controller;

import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.event.EventPage;
import io.github.vuhv48.flashsale.event.EventReasonRequest;
import io.github.vuhv48.flashsale.event.OrganizerEventCard;
import io.github.vuhv48.flashsale.event.OrganizerEventDetail;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
import io.github.vuhv48.flashsale.service.impl.EventQueryService;
import io.github.vuhv48.flashsale.service.impl.EventReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Duyệt sự kiện", description = "Admin duyệt, từ chối, tạm dừng hoặc hủy sự kiện")
@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

	private final EventQueryService eventQueryService;
	private final EventReviewService eventReviewService;

	@Operation(summary = "Danh sách sự kiện", description = "Lọc theo trạng thái. Bỏ trống thì lấy mọi trạng thái. Cần READ_UNPUBLISHED_EVENT.")
	@PreAuthorize("hasAuthority('READ_UNPUBLISHED_EVENT')")
	@GetMapping
	public EventPage<OrganizerEventCard> list(
			@Parameter(description = "DRAFT, PENDING_REVIEW, REJECTED, APPROVED, ON_SALE, PAUSED, SALE_ENDED, CANCELLED, COMPLETED")
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return eventQueryService.adminList(status, page, size);
	}

	@Operation(summary = "Chi tiết sự kiện", description = "Admin xem cả sự kiện chưa công khai, gồm loại vé và ảnh. Cần READ_UNPUBLISHED_EVENT.")
	@PreAuthorize("hasAuthority('READ_UNPUBLISHED_EVENT')")
	@GetMapping("/{id}")
	public OrganizerEventDetail detail(@PathVariable UUID id) {
		return eventQueryService.adminDetail(id);
	}

	@Operation(summary = "Duyệt sự kiện", description = "Chỉ sự kiện đang chờ duyệt. Sau khi duyệt, sự kiện hiện công khai và tự chuyển sang đang bán khi tới giờ. Cần REVIEW_EVENT.")
	@PreAuthorize("hasAuthority('REVIEW_EVENT')")
	@PostMapping("/{id}/approve")
	public OrganizerEventDetail approve(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id) {
		AppUserDetails actor = require(user);
		return eventReviewService.approve(actor.getId(), actor.getUsername(), id);
	}

	@Operation(summary = "Từ chối sự kiện", description = "Bắt buộc lý do từ 10 ký tự. Ban tổ chức sửa rồi gửi lại. Cần REVIEW_EVENT.")
	@PreAuthorize("hasAuthority('REVIEW_EVENT')")
	@PostMapping("/{id}/reject")
	public OrganizerEventDetail reject(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id, @Valid @RequestBody EventReasonRequest request) {
		AppUserDetails actor = require(user);
		return eventReviewService.reject(actor.getId(), actor.getUsername(), id, request);
	}

	@Operation(summary = "Admin tạm dừng đợt bán", description = "Không tính vào 3 lần tạm dừng của ban tổ chức. Cần PAUSE_SALE.")
	@PreAuthorize("hasAuthority('PAUSE_SALE')")
	@PostMapping("/{id}/pause")
	public OrganizerEventDetail pause(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id) {
		AppUserDetails actor = require(user);
		return eventReviewService.pause(actor.getId(), actor.getUsername(), id, true);
	}

	@Operation(summary = "Admin tiếp tục đợt bán", description = "Cần PAUSE_SALE.")
	@PreAuthorize("hasAuthority('PAUSE_SALE')")
	@PostMapping("/{id}/resume")
	public OrganizerEventDetail resume(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id) {
		AppUserDetails actor = require(user);
		return eventReviewService.resume(actor.getId(), actor.getUsername(), id, true);
	}

	@Operation(summary = "Hủy sự kiện", description = "Hủy giữ chỗ đang có, tạo yêu cầu hoàn tiền cho đơn đã thanh toán và vô hiệu hóa vé. Không hủy sự kiện đã diễn ra. Cần CANCEL_EVENT.")
	@PreAuthorize("hasAuthority('CANCEL_EVENT')")
	@PostMapping("/{id}/cancel")
	public OrganizerEventDetail cancel(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id, @Valid @RequestBody EventReasonRequest request) {
		AppUserDetails actor = require(user);
		return eventReviewService.cancel(actor.getId(), actor.getUsername(), id, request);
	}

	private static AppUserDetails require(AppUserDetails user) {
		if (user == null) {
			throw new DomainException(ErrorStatus.UNAUTHORIZED, ErrorStatus.UNAUTHORIZED.defaultMessage());
		}
		return user;
	}
}
