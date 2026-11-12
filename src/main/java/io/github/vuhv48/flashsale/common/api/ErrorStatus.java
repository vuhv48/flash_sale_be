package io.github.vuhv48.flashsale.common.api;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ErrorStatus {

	AUTH_FAILED(401, "AUTH_FAILED", "Email hoặc mật khẩu không đúng."),
	LOGIN_LOCKED(429, "LOGIN_LOCKED", "Bạn đã nhập sai quá 5 lần. Vui lòng thử lại sau 15 phút."),
	UNAUTHORIZED(401, "UNAUTHORIZED", "Chưa xác thực"),
	REFRESH_TOKEN_INVALID(401, "REFRESH_TOKEN_INVALID", "Refresh token không hợp lệ hoặc đã hết hạn"),
	FORBIDDEN(403, "FORBIDDEN", "Không đủ quyền"),
	TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "Bạn thao tác quá nhanh, vui lòng thử lại sau"),

	VALIDATION_ERROR(400, "VALIDATION_ERROR", "Dữ liệu không hợp lệ"),
	CONSTRAINT_VIOLATION(400, "CONSTRAINT_VIOLATION", "Vi phạm ràng buộc dữ liệu"),
	INVALID_ARGUMENT(400, "INVALID_ARGUMENT", "Tham số không hợp lệ"),
	PASSWORD_MISMATCH(400, "PASSWORD_MISMATCH", "Mật khẩu hiện tại không đúng"),
	USER_NOT_FOUND(404, "USER_NOT_FOUND", "Không tìm thấy người dùng"),
	ACCOUNT_DISABLED(403, "ACCOUNT_DISABLED", "Tài khoản đã bị khóa. Liên hệ hỗ trợ qua mục Chat để biết thêm."),

	USERNAME_TAKEN(409, "USERNAME_TAKEN", "Tên đăng nhập đã được sử dụng"),
	EMAIL_TAKEN(409, "EMAIL_TAKEN", "Email đã được sử dụng."),
	PHONE_TAKEN(409, "PHONE_TAKEN", "Số điện thoại đã được sử dụng."),
	RESET_TOKEN_INVALID(400, "RESET_TOKEN_INVALID", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."),
	CANNOT_LOCK_SELF(409, "CANNOT_LOCK_SELF", "Admin không được tự khóa tài khoản của chính mình."),
	ROLE_NOT_FOUND(404, "ROLE_NOT_FOUND", "Không tìm thấy vai trò"),
	ROLE_REQUEST_NOT_FOUND(404, "ROLE_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu vai trò"),
	ROLE_NOT_REQUESTABLE(409, "ROLE_NOT_REQUESTABLE", "Vai trò này không được phép xin"),
	ROLE_REQUEST_PENDING(409, "ROLE_REQUEST_PENDING", "Bạn đã có một yêu cầu đang chờ duyệt cho vai trò này."),
	ROLE_REQUEST_NOT_PENDING(409, "ROLE_REQUEST_NOT_PENDING", "Chỉ xử lý được yêu cầu đang chờ duyệt."),
	ROLE_ALREADY_ASSIGNED(409, "ROLE_ALREADY_ASSIGNED", "Bạn đã có vai trò này."),
	ROLE_NOT_ASSIGNED(409, "ROLE_NOT_ASSIGNED", "Bạn chưa có vai trò này."),
	LAST_ROLE(409, "LAST_ROLE", "Tài khoản phải còn ít nhất một vai trò."),
	LAST_ADMIN(409, "LAST_ADMIN", "Không gỡ được quản trị viên cuối cùng."),
	CANNOT_REMOVE_OWN_ADMIN(409, "CANNOT_REMOVE_OWN_ADMIN", "Admin không tự gỡ quyền ADMIN của chính mình."),
	ORGANIZER_HAS_OPEN_EVENTS(409, "ORGANIZER_HAS_OPEN_EVENTS", "Không duyệt gỡ vai trò ban tổ chức khi còn sự kiện chưa kết thúc."),
	TAX_CODE_TAKEN(409, "TAX_CODE_TAKEN", "Mã số thuế đã được sử dụng."),
	DATA_INTEGRITY(409, "DATA_INTEGRITY", "Dữ liệu xung đột ràng buộc (trùng khóa, FK, …)"),
	ILLEGAL_STATE(409, "ILLEGAL_STATE", "Trạng thái không hợp lệ"),

	DATA_ACCESS_ERROR(500, "DATA_ACCESS_ERROR", "Không thực hiện được thao tác dữ liệu"),
	INTERNAL_ERROR(500, "INTERNAL_ERROR", "Lỗi hệ thống không mong đợi");

	private static final Map<String, ErrorStatus> BY_CODE = Arrays.stream(values())
			.collect(Collectors.toUnmodifiableMap(ErrorStatus::code, Function.identity()));

	private final int httpStatus;
	private final String code;
	private final String defaultMessage;

	ErrorStatus(int httpStatus, String code, String defaultMessage) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.defaultMessage = defaultMessage;
	}

	public int httpStatus() {
		return httpStatus;
	}

	public String code() {
		return code;
	}

	public String defaultMessage() {
		return defaultMessage;
	}

	public static Optional<ErrorStatus> resolve(String code) {
		if (code == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(BY_CODE.get(code));
	}
}
