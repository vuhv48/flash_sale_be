package io.github.vuhv48.flashsale.service;

public interface LoginRateLimitService {

	boolean allowLoginAttempt(String clientIp);

	boolean allowRegisterAttempt(String clientIp);

	boolean isLoginLocked(String email);

	void recordLoginFailure(String email);

	void clearLoginFailures(String email);
}
