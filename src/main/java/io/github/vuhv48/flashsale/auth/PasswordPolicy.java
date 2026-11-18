package io.github.vuhv48.flashsale.auth;

import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;

public final class PasswordPolicy {

	private PasswordPolicy() {
	}

	public static void check(String password, String email) {
		boolean lengthOk = password != null && password.length() >= 8 && password.length() <= 64;
		boolean upper = password != null && password.chars().anyMatch(Character::isUpperCase);
		boolean lower = password != null && password.chars().anyMatch(Character::isLowerCase);
		boolean digit = password != null && password.chars().anyMatch(Character::isDigit);
		if (!lengthOk || !upper || !lower || !digit) {
			throw new DomainException(
					ErrorStatus.VALIDATION_ERROR,
					"Mật khẩu phải từ 8 đến 64 ký tự, có chữ hoa, chữ thường và số");
		}
		if (email != null && password.equalsIgnoreCase(email)) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Mật khẩu không được trùng email");
		}
	}
}
