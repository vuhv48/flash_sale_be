package io.github.vuhv48.flashsale.controller;

import io.github.vuhv48.flashsale.event.CategoryItem;
import io.github.vuhv48.flashsale.event.ProvinceItem;
import io.github.vuhv48.flashsale.service.impl.EventCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Danh mục sự kiện", description = "Tỉnh thành và thể loại dùng khi tạo sự kiện")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EventCatalogController {

	private final EventCatalogService eventCatalogService;

	@Operation(summary = "Danh sách tỉnh, thành phố", description = "34 tỉnh, thành theo danh mục. Dùng id khi tạo sự kiện. Không cần đăng nhập.")
	@GetMapping("/provinces")
	public List<ProvinceItem> provinces() {
		return eventCatalogService.provinces();
	}

	@Operation(summary = "Thể loại sự kiện", description = "MUSIC, SPORT, CONFERENCE, THEATER, OTHER. Không cần đăng nhập.")
	@GetMapping("/event-categories")
	public List<CategoryItem> categories() {
		return eventCatalogService.categories();
	}
}
