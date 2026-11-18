package io.github.vuhv48.flashsale.controller;

import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.event.AttachEventImageRequest;
import io.github.vuhv48.flashsale.event.EventPage;
import io.github.vuhv48.flashsale.event.OrganizerEventCard;
import io.github.vuhv48.flashsale.event.OrganizerEventDetail;
import io.github.vuhv48.flashsale.event.SaveEventRequest;
import io.github.vuhv48.flashsale.event.SaveTicketTypeRequest;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
import io.github.vuhv48.flashsale.service.impl.EventCommandService;
import io.github.vuhv48.flashsale.service.impl.EventQueryService;
import io.github.vuhv48.flashsale.service.impl.EventReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Sự kiện của ban tổ chức", description = "Tạo sự kiện nháp, loại vé, ảnh, gửi duyệt, tạm dừng đợt bán")
@RestController
@RequestMapping("/api/organizer/events")
@RequiredArgsConstructor
public class OrganizerEventController {

	private final EventCommandService eventCommandService;
	private final EventQueryService eventQueryService;
	private final EventReviewService eventReviewService;

	@Operation(summary = "Sự kiện của tôi", description = "Mọi trạng thái của ban tổ chức đang đăng nhập, mới nhất trước. Cần READ_UNPUBLISHED_EVENT.")
	@PreAuthorize("hasAuthority('READ_UNPUBLISHED_EVENT')")
	@GetMapping
	public EventPage<OrganizerEventCard> list(
			@AuthenticationPrincipal AppUserDetails user,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return eventQueryService.organizerList(id(user), page, size);
	}

	@Operation(summary = "Chi tiết sự kiện của tôi", description = "Gồm loại vé, số lượng và ảnh. Sự kiện của người khác trả 404. Cần READ_UNPUBLISHED_EVENT.")
	@PreAuthorize("hasAuthority('READ_UNPUBLISHED_EVENT')")
	@GetMapping("/{id}")
	public OrganizerEventDetail detail(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id) {
		return eventQueryService.organizerDetail(id(user), id);
	}

	@Operation(summary = "Tạo sự kiện nháp", description = "Tên là bắt buộc. Mô tả, địa điểm, giờ và loại vé có thể thêm sau, nhưng phải đủ trước khi gửi duyệt. Cần CREATE_DRAFT_EVENT.")
	@PreAuthorize("hasAuthority('CREATE_DRAFT_EVENT')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrganizerEventDetail create(@AuthenticationPrincipal AppUserDetails user, @Valid @RequestBody SaveEventRequest request) {
		return eventCommandService.create(id(user), user.getUsername(), request);
	}

	@Operation(summary = "Sửa sự kiện nháp hoặc bị từ chối", description = "Sự kiện bị từ chối quay về nháp. Sau khi duyệt không sửa giá, số lượng, giới hạn vé hoặc giờ mở bán. Cần UPDATE_OWN_EVENT.")
	@PreAuthorize("hasAuthority('UPDATE_OWN_EVENT')")
	@PutMapping("/{id}")
	public OrganizerEventDetail update(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id, @Valid @RequestBody SaveEventRequest request) {
		return eventCommandService.update(id(user), user.getUsername(), id, request);
	}

	@Operation(summary = "Thêm loại vé", description = "Tối đa 10 loại. Giá là bội số của 1.000, từ 0 đến 50.000.000. Cần CREATE_DRAFT_EVENT.")
	@PreAuthorize("hasAuthority('CREATE_DRAFT_EVENT')")
	@PostMapping("/{id}/ticket-types")
	@ResponseStatus(HttpStatus.CREATED)
	public OrganizerEventDetail addTicket(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id, @Valid @RequestBody SaveTicketTypeRequest request) {
		return eventCommandService.addTicket(id(user), user.getUsername(), id, request);
	}

	@Operation(summary = "Sửa loại vé", description = "Chỉ khi sự kiện đang nháp hoặc bị từ chối. Cần UPDATE_OWN_EVENT.")
	@PreAuthorize("hasAuthority('UPDATE_OWN_EVENT')")
	@PutMapping("/{id}/ticket-types/{ticketTypeId}")
	public OrganizerEventDetail updateTicket(
			@AuthenticationPrincipal AppUserDetails user,
			@PathVariable UUID id,
			@PathVariable UUID ticketTypeId,
			@Valid @RequestBody SaveTicketTypeRequest request
	) {
		return eventCommandService.updateTicket(id(user), user.getUsername(), id, ticketTypeId, request);
	}

	@Operation(summary = "Xóa loại vé", description = "Xóa mềm. Không xóa loại vé đã có vé đang giữ hoặc đã bán. Cần UPDATE_OWN_EVENT.")
	@PreAuthorize("hasAuthority('UPDATE_OWN_EVENT')")
	@DeleteMapping("/{id}/ticket-types/{ticketTypeId}")
	public OrganizerEventDetail deleteTicket(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id, @PathVariable UUID ticketTypeId) {
		return eventCommandService.deleteTicket(id(user), user.getUsername(), id, ticketTypeId);
	}

	@Operation(summary = "Gắn ảnh bìa hoặc ảnh phụ", description = "EVENT_COVER tối đa 1, EVENT_IMAGE tối đa 10. API ghi nhận thông tin ảnh để gửi duyệt. Tải tệp lên kho lưu trữ thuộc chức năng tệp. Cần UPDATE_OWN_EVENT.")
	@PreAuthorize("hasAuthority('UPDATE_OWN_EVENT')")
	@PostMapping("/{id}/images")
	@ResponseStatus(HttpStatus.CREATED)
	public OrganizerEventDetail attachImage(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id, @Valid @RequestBody AttachEventImageRequest request) {
		return eventCommandService.attachImage(id(user), user.getUsername(), id, request);
	}

	@Operation(summary = "Gỡ ảnh", description = "Id là id của ảnh trong sự kiện, không phải id tệp. Cần UPDATE_OWN_EVENT.")
	@PreAuthorize("hasAuthority('UPDATE_OWN_EVENT')")
	@DeleteMapping("/{id}/images/{imageId}")
	public OrganizerEventDetail deleteImage(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id, @PathVariable UUID imageId) {
		return eventCommandService.deleteImage(id(user), user.getUsername(), id, imageId);
	}

	@Operation(summary = "Gửi sự kiện đi duyệt", description = "Kiểm tra mô tả, thể loại, địa điểm, giờ, ít nhất một loại vé và một ảnh bìa. Giờ mở bán phải sau lúc gửi ít nhất 24 giờ. Cần SUBMIT_EVENT.")
	@PreAuthorize("hasAuthority('SUBMIT_EVENT')")
	@PostMapping("/{id}/submit")
	public OrganizerEventDetail submit(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id) {
		return eventCommandService.submit(id(user), user.getUsername(), id);
	}

	@Operation(summary = "Tạm dừng đợt bán", description = "Chỉ khi đang bán. Ban tổ chức tối đa 3 lần, mỗi lần tối đa 30 phút rồi tự bán tiếp. Cần PAUSE_SALE.")
	@PreAuthorize("hasAuthority('PAUSE_SALE')")
	@PostMapping("/{id}/pause")
	public OrganizerEventDetail pause(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id) {
		return eventReviewService.pause(id(user), user.getUsername(), id, false);
	}

	@Operation(summary = "Tiếp tục đợt bán", description = "Từ tạm dừng về đang bán. Nếu đã hết giờ bán hoặc hết vé thì chuyển sang kết thúc bán. Cần PAUSE_SALE.")
	@PreAuthorize("hasAuthority('PAUSE_SALE')")
	@PostMapping("/{id}/resume")
	public OrganizerEventDetail resume(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID id) {
		return eventReviewService.resume(id(user), user.getUsername(), id, false);
	}

	private static UUID id(AppUserDetails user) {
		if (user == null) {
			throw new DomainException(ErrorStatus.UNAUTHORIZED, ErrorStatus.UNAUTHORIZED.defaultMessage());
		}
		return user.getId();
	}
}
