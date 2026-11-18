package io.github.vuhv48.flashsale.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class EventCache {

	static final Duration TTL = Duration.ofSeconds(30);
	private static final String VERSION = "flash-sale:events:list-version";
	private static final String DETAIL = "flash-sale:events:detail:";
	private static final String INVENTORY = "flash-sale:inventory:";

	private final ObjectProvider<StringRedisTemplate> redisProvider;

	public EventCache(ObjectProvider<StringRedisTemplate> redisProvider) {
		this.redisProvider = redisProvider;
	}

	public String listVersion() {
		StringRedisTemplate redis = redisProvider.getIfAvailable();
		if (redis == null) {
			return "0";
		}
		try {
			String value = redis.opsForValue().get(VERSION);
			return value == null ? "0" : value;
		} catch (RuntimeException ex) {
			log.warn("[event] Redis không đọc được phiên bản danh sách: {}", ex.getMessage());
			return "0";
		}
	}

	public String get(String key) {
		StringRedisTemplate redis = redisProvider.getIfAvailable();
		if (redis == null) {
			return null;
		}
		try {
			return redis.opsForValue().get(key);
		} catch (RuntimeException ex) {
			log.warn("[event] Redis không đọc được cache: {}", ex.getMessage());
			return null;
		}
	}

	public void put(String key, String json) {
		StringRedisTemplate redis = redisProvider.getIfAvailable();
		if (redis == null) {
			return;
		}
		try {
			redis.opsForValue().set(key, json, TTL);
		} catch (RuntimeException ex) {
			log.warn("[event] Redis không ghi được cache: {}", ex.getMessage());
		}
	}

	public void evict(UUID eventId) {
		StringRedisTemplate redis = redisProvider.getIfAvailable();
		if (redis == null) {
			return;
		}
		try {
			redis.delete(DETAIL + eventId);
			redis.opsForValue().increment(VERSION);
		} catch (RuntimeException ex) {
			log.warn("[event] Redis không xóa được cache sự kiện: {}", ex.getMessage());
		}
	}

	public void seedInventory(UUID ticketTypeId, int available) {
		StringRedisTemplate redis = redisProvider.getIfAvailable();
		if (redis == null) {
			return;
		}
		try {
			redis.opsForValue().setIfAbsent(INVENTORY + ticketTypeId, Integer.toString(Math.max(available, 0)));
		} catch (RuntimeException ex) {
			log.warn("[event] Redis không nạp được tồn kho: {}", ex.getMessage());
		}
	}

	public static String detailKey(UUID eventId) {
		return DETAIL + eventId;
	}
}
