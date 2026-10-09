package com.app.ideaforge.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.security.AuthUser;
import com.app.ideaforge.security.HashingService;
import com.app.ideaforge.user.User;
import com.app.ideaforge.user.UserRepository;

/**
 * Opaque session tokens: 256-bit random, only the SHA-256 hash is stored.
 * Sliding expiry (idle days) capped by an absolute lifetime.
 */
@Service
public class SessionService {

	/** Do not write to the DB on every request; refresh at most hourly. */
	private static final Duration TOUCH_INTERVAL = Duration.ofHours(1);

	private final AuthSessionRepository sessions;
	private final UserRepository users;
	private final HashingService hashingService;
	private final AppProperties properties;
	private final Clock clock;

	public SessionService(
			AuthSessionRepository sessions,
			UserRepository users,
			HashingService hashingService,
			AppProperties properties,
			Clock clock) {
		this.sessions = sessions;
		this.users = users;
		this.hashingService = hashingService;
		this.properties = properties;
		this.clock = clock;
	}

	public record CreatedSession(String token, Instant expiresAt) {
	}

	public record Resolution(AuthUser user, boolean extended, Instant expiresAt) {
	}

	@Transactional
	public CreatedSession create(User user, String ipHash, String userAgent) {
		String token = hashingService.randomToken();
		Instant now = clock.instant();
		Instant expiresAt = now.plus(Duration.ofDays(properties.auth().session().idleDays()));
		AuthSession session = new AuthSession(
				user.getId(),
				hashingService.sha256Hex(token),
				ipHash,
				truncate(userAgent, 500),
				expiresAt);
		sessions.save(session);
		return new CreatedSession(token, expiresAt);
	}

	@Transactional
	public Optional<Resolution> resolve(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		Instant now = clock.instant();
		Optional<AuthSession> found = sessions.findByTokenHash(hashingService.sha256Hex(token));
		if (found.isEmpty()) {
			return Optional.empty();
		}
		AuthSession session = found.get();
		Instant absoluteCap = session.getCreatedAt().plus(Duration.ofDays(properties.auth().session().absoluteDays()));
		if (!session.isValid(now) || !absoluteCap.isAfter(now)) {
			return Optional.empty();
		}
		Optional<User> user = users.findById(session.getUserId());
		if (user.isEmpty() || !user.get().isActive()) {
			return Optional.empty();
		}

		boolean extended = false;
		Instant expiresAt = session.getExpiresAt();
		if (session.getLastSeenAt().isBefore(now.minus(TOUCH_INTERVAL))) {
			Instant slid = now.plus(Duration.ofDays(properties.auth().session().idleDays()));
			if (slid.isBefore(absoluteCap)) {
				expiresAt = slid;
			} else {
				expiresAt = absoluteCap;
			}
			session.touch(now, expiresAt);
			extended = true;
		}
		User principal = user.get();
		return Optional.of(new Resolution(
				new AuthUser(principal.getId(), principal.getEmail(), principal.getRole().name()),
				extended,
				expiresAt));
	}

	@Transactional
	public void revoke(String token) {
		if (token == null || token.isBlank()) {
			return;
		}
		sessions.findByTokenHash(hashingService.sha256Hex(token)).ifPresent(session -> {
			if (session.getRevokedAt() == null) {
				session.revoke(clock.instant());
			}
		});
	}

	@Transactional
	public void revokeForUser(UUID userId) {
		sessions.revokeAllForUser(userId, clock.instant());
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		return value.length() <= max ? value : value.substring(0, max);
	}
}
