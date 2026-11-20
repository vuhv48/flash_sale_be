package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Loại vé của sự kiện")
public record SaveTicketTypeRequest(
		@Schema(description = "Tên loại vé, không trùng trong sự kiện", example = "VIP")
		@NotBlank(message = "Tên loại vé không được để trống")
		@Size(min = 2, max = 50, message = "Tên loại vé phải từ 2 đến 50 ký tự")
		String name,
		@Schema(description = "Giá một vé, 0 đến 50.000.000, bội số của 1.000. 0 là miễn phí", example = "500000")
		@NotNull(message = "Giá không được để trống")
		@Min(value = 0, message = "Giá từ 0 đến 50.000.000")
		@Max(value = 50_000_000, message = "Giá từ 0 đến 50.000.000")
		Long price,
		@Schema(description = "Tổng số vé phát hành, 1-100.000", example = "1000")
		@NotNull(message = "Tổng số lượng không được để trống")
		@Min(value = 1, message = "Tổng số lượng từ 1 đến 100.000")
		@Max(value = 100_000, message = "Tổng số lượng từ 1 đến 100.000")
		Integer totalQuantity,
		@Schema(description = "Thứ tự hiển thị", example = "1")
		@Min(value = 0, message = "Thứ tự không được âm")
		Integer sortOrder
) {
	public SaveTicketTypeRequest {
		if (name != null) {
			name = name.trim();
		}
	}
}
