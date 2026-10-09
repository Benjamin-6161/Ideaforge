package com.app.ideaforge.user;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.security.AuthUser;

/**
 * User lookup/creation. Creation always goes through signup throttling and
 * publishes {@link UserVerifiedEvent} for the signup credit grant.
 */
@Service
public class UserService {

	private final UserRepository users;
	private final UserIdentityRepository identities;
	private final SignupThrottleService signupThrottle;
	private final ApplicationEventPublisher events;
	private final EmailNormalizer emailNormalizer;
	private final Clock clock;

	public UserService(
			UserRepository users,
			UserIdentityRepository identities,
			SignupThrottleService signupThrottle,
			ApplicationEventPublisher events,
			EmailNormalizer emailNormalizer,
			Clock clock) {
		this.users = users;
		this.identities = identities;
		this.signupThrottle = signupThrottle;
		this.events = events;
		this.emailNormalizer = emailNormalizer;
		this.clock = clock;
	}

	@Transactional
	public User findOrCreateVerifiedUser(
			String email,
			String ipHash,
			IdentityProvider provider,
			String providerSubject,
			String displayName,
			String avatarUrl) {
		String normalized = emailNormalizer.normalize(email);
		Optional<User> existing = users.findByEmailNormalized(normalized);
		if (existing.isPresent()) {
			User user = existing.get();
			if (identities.findByProviderAndProviderSubject(provider, providerSubject).isEmpty()) {
				identities.save(new UserIdentity(user.getId(), provider, providerSubject));
			}
			user.setLastLoginAt(clock.instant());
			return user;
		}

		signupThrottle.assertAllowed(ipHash);
		User user = new User(email.trim(), normalized, ipHash);
		user.setDisplayName(displayName);
		user.setAvatarUrl(avatarUrl);
		user.setEmailVerifiedAt(clock.instant());
		user.setLastLoginAt(clock.instant());
		users.save(user);
		identities.save(new UserIdentity(user.getId(), provider, providerSubject));
		events.publishEvent(new UserVerifiedEvent(user.getId(), true));
		return user;
	}

	@Transactional(readOnly = true)
	public Optional<User> findById(UUID id) {
		return users.findById(id);
	}

	@Transactional(readOnly = true)
	public Optional<User> findByEmailNormalized(String emailNormalized) {
		return users.findByEmailNormalized(emailNormalized);
	}

	@Transactional(readOnly = true)
	public AuthUser toAuthUser(User user) {
		return new AuthUser(user.getId(), user.getEmail(), user.getRole().name());
	}

	public void requireActive(User user) {
		if (!user.isActive()) {
			throw ApiException.forbidden(ErrorCodes.ACCOUNT_SUSPENDED, "This account is suspended.");
		}
	}
}
