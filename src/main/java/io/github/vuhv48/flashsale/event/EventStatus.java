package io.github.vuhv48.flashsale.event;

import java.util.Set;

public final class EventStatus {

	public static final String DRAFT = "DRAFT";
	public static final String PENDING_REVIEW = "PENDING_REVIEW";
	public static final String REJECTED = "REJECTED";
	public static final String APPROVED = "APPROVED";
	public static final String ON_SALE = "ON_SALE";
	public static final String PAUSED = "PAUSED";
	public static final String SALE_ENDED = "SALE_ENDED";
	public static final String CANCELLED = "CANCELLED";
	public static final String COMPLETED = "COMPLETED";

	public static final Set<String> PUBLIC = Set.of(APPROVED, ON_SALE, PAUSED, SALE_ENDED);
	public static final Set<String> EDITABLE = Set.of(DRAFT, REJECTED);
	public static final Set<String> FINISHED = Set.of(CANCELLED, COMPLETED);

	private EventStatus() {
	}

	public static String label(String status) {
		return switch (status) {
			case DRAFT -> "Nháp";
			case PENDING_REVIEW -> "Chờ duyệt";
			case REJECTED -> "Bị từ chối";
			case APPROVED -> "Đã duyệt, sắp mở bán";
			case ON_SALE -> "Đang bán";
			case PAUSED -> "Tạm dừng";
			case SALE_ENDED -> "Kết thúc bán";
			case CANCELLED -> "Đã hủy";
			case COMPLETED -> "Đã diễn ra";
			default -> status;
		};
	}

	public static boolean isPublic(String status) {
		return PUBLIC.contains(status);
	}

	public static boolean isEditable(String status) {
		return EDITABLE.contains(status);
	}
}
