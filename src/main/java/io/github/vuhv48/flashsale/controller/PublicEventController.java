package io.github.vuhv48.flashsale.controller;

import io.github.vuhv48.flashsale.event.EventPage;
import io.github.vuhv48.flashsale.event.PublicEventCard;
import io.github.vuhv48.flashsale.event.PublicEventDetail;
import io.github.vuhv48.flashsale.service.impl.EventQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Sự kiện công khai", description = "Khách và người chưa đăng nhập xem sự kiện đã duyệt")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class PublicEventController {

	private final EventQueryService eventQueryService;

	@Operation(
			summary = "Danh sách sự kiện công khai",
			description = "Sự kiện đã duyệt, đang bán, tạm dừng hoặc đã kết thúc bán. Lọc theo thể loại, tỉnh thành và ngày diễn ra (giờ Việt Nam). Sắp theo giờ mở bán. Không trả số vé còn lại."
	)
	@GetMapping
	public EventPage<PublicEventCard> list(
			@Parameter(description = "MUSIC, SPORT, CONFERENCE, THEATER, OTHER", example = "MUSIC")
			@RequestParam(required = false) String category,
			@Parameter(description = "Id tỉnh, thành")
			@RequestParam(required = false) UUID provinceId,
			@Parameter(description = "Ngày sự kiện bắt đầu, yyyy-MM-dd", example = "2026-12-20")
			@RequestParam(required = false) LocalDate date,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return eventQueryService.publicList(category, provinceId, date, page, size);
	}

	@Operation(
			summary = "Chi tiết sự kiện công khai",
			description = "Thông tin, loại vé, trạng thái Còn / Sắp hết / Hết và đồng hồ đếm ngược. Không hiện số vé chính xác."
	)
	@GetMapping("/{id}")
	public PublicEventDetail detail(@PathVariable UUID id) {
		return eventQueryService.publicDetail(id);
	}
}
