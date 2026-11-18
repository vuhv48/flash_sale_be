package io.github.vuhv48.flashsale.event;

public final class TicketAvailability {

	public static final String AVAILABLE = "AVAILABLE";
	public static final String LOW = "LOW";
	public static final String SOLD_OUT = "SOLD_OUT";

	private TicketAvailability() {
	}

	public static String code(int total, int available) {
		if (total <= 0 || available <= 0) {
			return SOLD_OUT;
		}
		if (available * 100L <= total * 20L) {
			return LOW;
		}
		return AVAILABLE;
	}

	public static String label(String code) {
		return switch (code) {
			case LOW -> "Sắp hết";
			case SOLD_OUT -> "Hết";
			default -> "Còn";
		};
	}
}
