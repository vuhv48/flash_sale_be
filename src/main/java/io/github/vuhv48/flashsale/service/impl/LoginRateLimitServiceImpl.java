package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.service.LoginRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRateLimitServiceImpl implements LoginRateLimitService {

	private static final long LOGIN_LIMIT = 20;
	private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);
	private static final long REGISTER_LIMIT = 5;
	private static final Duration REGISTER_WINDOW = Duration.ofHours(1);
	private static final int FAILURES_BEFORE_LOCK = 5;
	private static final Duration LOCK_WINDOW = Duration.ofMinutes(15);

	private final ObjectProvider<StringRedisTemplate> stringRedisTemplate;

	@Override
	public boolean allowLoginAttempt(String clientIp) {
		return incrementWithin(loginRateKey(clientIp), LOGIN_LIMIT, LOGIN_WINDOW);
	}

	@Override
	public boolean allowRegisterAttempt(String clientIp) {
		return incrementWithin(registerRateKey(clientIp), REGISTER_LIMIT, REGISTER_WINDOW);
	}

	@Override
	public boolean isLoginLocked(String email) {
		StringRedisTemplate redis = stringRedisTemplate.getIfAvailable();
		if (redis == null) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(redis.hasKey(lockKey(email)));
		} catch (RuntimeException ex) {
			log.warn("[auth] Redis không đọc được khóa đăng nhập: {}", ex.getMessage());
			return false;
		}
	}

	@Override
	public void recordLoginFailure(String email) {
		StringRedisTemplate redis = stringRedisTemplate.getIfAvailable();
		if (redis == null) {
			return;
		}
		try {
			String failures = failureKey(email);
			Long count = redis.opsForValue().increment(failures);
			if (count != null && count == 1L) {
				redis.expire(failures, LOCK_WINDOW);
			}
			if (count != null && count >= FAILURES_BEFORE_LOCK) {
				redis.opsForValue().set(lockKey(email), "1", LOCK_WINDOW);
				redis.delete(failures);
			}
		} catch (RuntimeException ex) {
			log.warn("[auth] Redis không ghi được lần đăng nhập sai: {}", ex.getMessage());
		}
	}

	@Override
	public void clearLoginFailures(String email) {
		StringRedisTemplate redis = stringRedisTemplate.getIfAvailable();
		if (redis == null) {
			return;
		}
		try {
			redis.delete(failureKey(email));
			redis.delete(lockKey(email));
		} catch (RuntimeException ex) {
			log.warn("[auth] Redis không xóa được bộ đếm đăng nhập: {}", ex.getMessage());
		}
	}

	private boolean incrementWithin(String key, long limit, Duration window) {
		StringRedisTemplate redis = stringRedisTemplate.getIfAvailable();
		if (redis == null) {
			return true;
		}
		try {
			Long count = redis.opsForValue().increment(key);
			if (count == null) {
				return true;
			}
			if (count == 1L) {
				redis.expire(key, window);
			}
			return count <= limit;
		} catch (RuntimeException ex) {
			log.warn("[auth] Redis không đếm được request: {}", ex.getMessage());
			return true;
		}
	}

	private static String loginRateKey(String clientIp) {
		return "flash-sale:auth:login-rate:" + safe(clientIp);
	}

	private static String registerRateKey(String clientIp) {
		return "flash-sale:auth:register-rate:" + safe(clientIp);
	}

	private static String failureKey(String email) {
		return "flash-sale:auth:login-fail:" + safe(email).toLowerCase();
	}

	private static String lockKey(String email) {
		return "flash-sale:auth:login-lock:" + safe(email).toLowerCase();
	}

	private static String safe(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}
		return value.trim().replace(':', '_');
	}
}
