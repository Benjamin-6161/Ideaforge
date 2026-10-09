package com.app.ideaforge.user;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.security.SecurityEventService;

/**
 * Blocks too many new accounts from one IP hash in a rolling 24h window.
 * Raw IPs are never stored, only peppered hashes.
 */
@Service
public class SignupThrottleService {

	private final UserRepository userRepository;
	private final SecurityEventService securityEvents;
	private final AppProperties properties;
	private final Clock clock;

	public SignupThrottleService(
			UserRepository userRepository,
			SecurityEventService securityEvents,
			AppProperties properties,
			Clock clock) {
		this.userRepository = userRepository;
		this.securityEvents = securityEvents;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public void assertAllowed(String ipHash) {
		if (ipHash == null || ipHash.isBlank()) {
			return;
		}
		Instant since = clock.instant().minus(Duration.ofDays(1));
		long recentSignups = userRepository.countBySignupIpHashAndCreatedAtAfter(ipHash, since);
		int limit = properties.auth().signup().maxPerIpPerDay();
		if (recentSignups >= limit) {
			securityEvents.log("SIGNUP_THROTTLED", null, ipHash, Map.of("recentSignups", recentSignups));
			throw ApiException.forbidden(
					ErrorCodes.SIGNUP_BLOCKED,
					"Too many accounts have been created from this network. Please try again later.");
		}
	}
}
