package com.app.ideaforge.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * In-memory fixed-key rate limiter. Single instance target (one VPS), so a
 * local map is sufficient; keys are already hashed where they identify users.
 */
@Component
public class RateLimiterService {

	private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

	public boolean tryConsume(String key, int capacity, Duration window) {
		Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
				.addLimit(Bandwidth.builder()
						.capacity(capacity)
						.refillGreedy(capacity, window)
						.build())
				.build());
		return bucket.tryConsume(1);
	}

	/** Removes a bucket; used after successful OTP verification to clear cooldown. */
	public void reset(String key) {
		buckets.remove(key);
	}
}
