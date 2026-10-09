package com.app.ideaforge.auth;

import java.time.Clock;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.security.ClientIpHasher;
import com.app.ideaforge.security.RateLimiterService;
import com.app.ideaforge.security.SecurityEventService;
import com.app.ideaforge.security.TurnstileVerifier;
import com.app.ideaforge.user.IdentityProvider;
import com.app.ideaforge.user.User;
import com.app.ideaforge.user.UserService;

import jakarta.servlet.http.HttpServletRequest;

/** Google Identity Services sign-in: verified ID token -> session. */
@Service
public class GoogleAuthService {

	private final GoogleTokenVerifier googleTokenVerifier;
	private final TurnstileVerifier turnstileVerifier;
	private final UserService userService;
	private final SessionService sessionService;
	private final ClientIpHasher ipHasher;
	private final RateLimiterService rateLimiter;
	private final SecurityEventService securityEvents;
	private final AppProperties properties;
	private final Clock clock;

	public GoogleAuthService(
			GoogleTokenVerifier googleTokenVerifier,
			TurnstileVerifier turnstileVerifier,
			UserService userService,
			SessionService sessionService,
			ClientIpHasher ipHasher,
			RateLimiterService rateLimiter,
			SecurityEventService securityEvents,
			AppProperties properties,
			Clock clock) {
		this.googleTokenVerifier = googleTokenVerifier;
		this.turnstileVerifier = turnstileVerifier;
		this.userService = userService;
		this.sessionService = sessionService;
		this.ipHasher = ipHasher;
		this.rateLimiter = rateLimiter;
		this.securityEvents = securityEvents;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public AuthResult login(
			String idToken,
			String turnstileToken,
			HttpServletRequest request) {
		String ipHash = ipHasher.hash(request);

		if (!rateLimiter.tryConsume("google-ip:" + ipHash, 20, java.time.Duration.ofHours(1))) {
			securityEvents.log("RATE_LIMITED", null, ipHash, Map.of("flow", "google"));
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.OTP_RATE_LIMITED, "Too many attempts. Try later.");
		}
		if (!turnstileVerifier.verify(turnstileToken, ipHasher.resolveIp(request))) {
			securityEvents.log("TURNSTILE_FAILED", null, ipHash, Map.of("flow", "google"));
			throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.TURNSTILE_FAILED, "Verification failed. Please retry.");
		}

		GoogleTokenVerifier.GoogleAccount account = googleTokenVerifier.verify(idToken);
		User user = userService.findOrCreateVerifiedUser(
				account.email(),
				ipHash,
				IdentityProvider.GOOGLE,
				account.subject(),
				account.name(),
				account.picture());
		userService.requireActive(user);
		user.setLastLoginAt(clock.instant());
		SessionService.CreatedSession session = sessionService.create(user, ipHash, request.getHeader("User-Agent"));
		return new AuthResult(user, session.token(), session.expiresAt());
	}
}
