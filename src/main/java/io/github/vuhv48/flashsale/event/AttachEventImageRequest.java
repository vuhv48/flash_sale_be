package io.github.vuhv48.flashsale.event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Ghi nhận ảnh bìa hoặc ảnh phụ. Tệp thật được tải lên ở chức năng tệp; API này lưu thông tin ảnh để gửi duyệt.")
public record AttachEventImageRequest(
		@Schema(description = "EVENT_COVER hoặc EVENT_IMAGE", example = "EVENT_COVER")
		@NotBlank(message = "Loại ảnh không được để trống")
		String type,
		@Schema(description = "Tên tệp", example = "cover.jpg")
		@NotBlank(message = "Tên tệp không được để trống")
		@Size(max = 200, message = "Tên tệp tối đa 200 ký tự")
		String fileName,
		@Schema(description = "image/jpeg, image/png hoặc image/webp", example = "image/jpeg")
		@NotBlank(message = "Định dạng ảnh không được để trống")
		String mimeType,
		@Schema(description = "Kích thước tệp, byte, tối đa 5 MB", example = "350000")
		@NotNull(message = "Kích thước tệp không được để trống")
		@Min(value = 1, message = "Kích thước tệp phải lớn hơn 0")
		@Max(value = 5_242_880, message = "Ảnh tối đa 5 MB")
		Long sizeBytes,
		@Schema(description = "Thứ tự ảnh phụ", example = "1")
		@Min(value = 0, message = "Thứ tự không được âm")
		Integer sortOrder
) {
	public AttachEventImageRequest {
		if (type != null) {
			type = type.trim().toUpperCase();
		}
		if (fileName != null) {
			fileName = fileName.trim();
		}
		if (mimeType != null) {
			mimeType = mimeType.trim().toLowerCase();
		}
	}
}
