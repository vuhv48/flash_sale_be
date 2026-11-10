package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.service.LoginRateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginRateLimitServiceImpl implements LoginRateLimitService {

	private static final long MAX_REQUESTS_PER_WINDOW = 5;
	private static final Duration WINDOW = Duration.ofMinutes(1);

	private final ObjectProvider<StringRedisTemplate> stringRedisTemplate;

	@Override
	public boolean isAllowed(String clientIp) {
		StringRedisTemplate redis = stringRedisTemplate.getIfAvailable();
		if (redis == null) {
			return true;
		}
		String key = loginRateKey(clientIp);
		Long count = redis.opsForValue().increment(key);
		if (count == null) {
			return true;
		}
		if (count == 1L) {
			redis.expire(key, WINDOW);
		}
		return count <= MAX_REQUESTS_PER_WINDOW;
	}

	@Override
	public void clear(String clientIp) {
		StringRedisTemplate redis = stringRedisTemplate.getIfAvailable();
		if (redis == null) {
			return;
		}
		redis.delete(loginRateKey(clientIp));
	}

	private String loginRateKey(String clientIp) {
		String safeIp = (clientIp == null || clientIp.isBlank())
				? "unknown"
				: clientIp.trim().replace(':', '_');
		return "flash-sale:auth:login-rate:" + safeIp;
	}
}
