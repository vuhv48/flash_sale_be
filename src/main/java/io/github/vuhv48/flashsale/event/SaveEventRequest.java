package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Thông tin sự kiện khi tạo nháp hoặc cập nhật")
public record SaveEventRequest(
		@Schema(description = "Tên sự kiện, 5-200 ký tự", example = "Đêm nhạc Mùa Đông 2026")
		@NotBlank(message = "Tên sự kiện không được để trống")
		@Size(min = 5, max = 200, message = "Tên sự kiện phải từ 5 đến 200 ký tự")
		String name,
		@Schema(description = "Mô tả, 20-10000 ký tự khi gửi duyệt", example = "Đêm nhạc ngoài trời tại sân vận động, mở cửa từ 17:00.")
		@Size(max = 10000, message = "Mô tả tối đa 10.000 ký tự")
		String description,
		@Schema(description = "Mã thể loại: MUSIC, SPORT, CONFERENCE, THEATER, OTHER", example = "MUSIC")
		@Size(max = 50)
		String category,
		@Schema(description = "Tên địa điểm", example = "Sân vận động Mỹ Đình")
		@Size(max = 200)
		String venueName,
		@Schema(description = "Số nhà, đường, phường/xã", example = "Đường Lê Đức Thọ, Mỹ Đình")
		@Size(max = 300)
		String venueAddress,
		@Schema(description = "Id tỉnh, thành. Lấy từ GET /api/provinces", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
		UUID provinceId,
		@Schema(description = "Giờ sự kiện bắt đầu", example = "2026-12-20T12:00:00Z")
		Instant startsAt,
		@Schema(description = "Giờ sự kiện kết thúc", example = "2026-12-20T15:00:00Z")
		Instant endsAt,
		@Schema(description = "Giờ mở phòng chờ, trước giờ mở bán 5-60 phút", example = "2026-12-19T02:30:00Z")
		Instant waitingRoomOpensAt,
		@Schema(description = "Giờ mở bán, sau lúc gửi duyệt ít nhất 24 giờ", example = "2026-12-19T03:00:00Z")
		Instant saleStartsAt,
		@Schema(description = "Giờ đóng bán, trước hoặc bằng giờ bắt đầu", example = "2026-12-20T11:00:00Z")
		Instant saleEndsAt,
		@Schema(description = "Giới hạn vé mỗi tài khoản, 1-10. Bỏ trống thì là 4", example = "4")
		@Min(value = 1, message = "Giới hạn vé mỗi người từ 1 đến 10")
		@Max(value = 10, message = "Giới hạn vé mỗi người từ 1 đến 10")
		Integer maxTicketsPerUser
) {
	public SaveEventRequest {
		name = trim(name);
		description = blankToNull(description);
		category = blankToNull(category);
		venueName = blankToNull(venueName);
		venueAddress = blankToNull(venueAddress);
		if (category != null) {
			category = category.toUpperCase();
		}
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
