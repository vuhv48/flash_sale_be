package io.github.vuhv48.flashsale.service;

public interface LoginRateLimitService {

	boolean isAllowed(String clientIp);

	void clear(String clientIp);
}
