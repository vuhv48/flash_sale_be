package io.github.vuhv48.flashsale.auth;

public interface LoginRateLimitService {

	boolean isAllowed(String clientIp);

	void clear(String clientIp);
}
