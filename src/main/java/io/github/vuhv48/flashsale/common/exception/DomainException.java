package io.github.vuhv48.flashsale.common.exception;

import io.github.vuhv48.flashsale.common.api.ErrorStatus;

public class DomainException extends RuntimeException {

	private final String code;

	public DomainException(ErrorStatus status, String message) {
		super(message);
		this.code = status.code();
	}

	public String getCode() {
		return code;
	}
}
