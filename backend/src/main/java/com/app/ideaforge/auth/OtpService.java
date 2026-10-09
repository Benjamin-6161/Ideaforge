package com.app.ideaforge.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.security.ClientIpHasher;
import com.app.ideaforge.security.DisposableEmailChecker;
import com.app.ideaforge.security.HashingService;
import com.app.ideaforge.security.MxChecker;
import com.app.ideaforge.security.RateLimiterService;
import com.app.ideaforge.security.SecurityEventService;
import com.app.ideaforge.security.TurnstileVerifier;
import com.app.ideaforge.user.EmailNormalizer;
import com.app.ideaforge.user.IdentityProvider;
import com.app.ideaforge.user.User;
import com.app.ideaforge.user.UserService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Passwordless email OTP: HMAC-hashed codes, bounded attempts, expiry,
 * resend cooldown, per-email and per-IP rate limits, and generic responses that
 * never reveal whether an account exists.
 */
@Service
public class OtpService {

	private static final Logger log = LoggerFactory.getLogger(OtpService.class);

	private final OtpChallengeRepository challenges;
	private final UserService userService;
	private final EmailNormalizer emailNormalizer;
	private final EmailSender emailSender;
	private final DisposableEmailChecker disposableEmails;
	private final MxChecker mxChecker;
	private final RateLimiterService rateLimiter;
	private final SecurityEventService securityEvents;
	private final ClientIpHasher ipHasher;
	private final HashingService hashingService;
	private final SessionService sessionService;
	private final TurnstileVerifier turnstileVerifier;
	private final AppProperties properties;
	private final Environment environment;
	private final Clock clock;

	public OtpService(
			OtpChallengeRepository challenges,
			UserService userService,
			EmailNormalizer emailNormalizer,
			EmailSender emailSender,
			DisposableEmailChecker disposableEmails,
			MxChecker mxChecker,
			RateLimiterService rateLimiter,
			SecurityEventService securityEvents,
			ClientIpHasher ipHasher,
			HashingService hashingService,
			SessionService sessionService,
			TurnstileVerifier turnstileVerifier,
			AppProperties properties,
			Environment environment,
			Clock clock) {
		this.challenges = challenges;
		this.userService = userService;
		this.emailNormalizer = emailNormalizer;
		this.emailSender = emailSender;
		this.disposableEmails = disposableEmails;
		this.mxChecker = mxChecker;
		this.rateLimiter = rateLimiter;
		this.securityEvents = securityEvents;
		this.ipHasher = ipHasher;
		this.hashingService = hashingService;
		this.sessionService = sessionService;
		this.turnstileVerifier = turnstileVerifier;
		this.properties = properties;
		this.environment = environment;
		this.clock = clock;
	}

	/**
	 * Always completes without error (callers return a generic 202). Silently
	 * drops requests that fail Turnstile, email, or rate-limit checks.
	 */
	@Transactional
	public void requestCode(String rawEmail, String turnstileToken, HttpServletRequest request) {
		String normalized = emailNormalizer.normalize(rawEmail);
		String ipHash = ipHasher.hash(request);
		String domain = emailNormalizer.domainOf(rawEmail);

		if (!turnstileVerifier.verify(turnstileToken, ipHasher.resolveIp(request))) {
			securityEvents.log("TURNSTILE_FAILED", null, ipHash, Map.of("flow", "otp_request"));
			return;
		}
		if (disposableEmails.isDisposable(rawEmail)) {
			securityEvents.log("DISPOSABLE_EMAIL_BLOCKED", null, ipHash, Map.of("domain", domain));
			return;
		}
		if (!mxChecker.hasMailTarget(domain)) {
			securityEvents.log("EMAIL_MX_REJECTED", null, ipHash, Map.of("domain", domain));
			return;
		}

		AppProperties.Auth.Otp otp = properties.auth().otp();
		boolean emailAllowed = rateLimiter.tryConsume(
				"otp-email:" + hashingService.hmacSha256Hex(properties.security().otpPepper(), normalized),
				otp.maxRequestsPerEmailPerHour(),
				Duration.ofHours(1));
		boolean ipAllowed = rateLimiter.tryConsume(
				"otp-ip:" + ipHash,
				otp.maxRequestsPerIpPerHour(),
				Duration.ofHours(1));
		if (!emailAllowed || !ipAllowed) {
			securityEvents.log("OTP_RATE_LIMITED", null, ipHash,
					Map.of("emailAllowed", emailAllowed, "ipAllowed", ipAllowed));
			return;
		}

		Instant now = clock.instant();
		Optional<OtpChallenge> last = challenges.findTopByEmailNormalizedOrderByCreatedAtDesc(normalized);
		if (last.isPresent()
				&& last.get().getCreatedAt().isAfter(now.minusSeconds(otp.resendCooldownSeconds()))) {
			return;
		}

		challenges.consumeAllActive(normalized, now);
		String code = isDevFixedOtpActive() ? properties.auth().devFixedOtp() : hashingService.randomDigits(6);
		String codeHash = hashCode(normalized, code);
		OtpChallenge challenge = new OtpChallenge(
				normalized,
				codeHash,
				otp.maxAttempts(),
				now.plus(Duration.ofMinutes(otp.ttlMinutes())),
				ipHash);
		challenges.save(challenge);
		emailSender.sendOtp(rawEmail.trim(), code, otp.ttlMinutes());
		log.debug("OTP challenge created for {}", mask(rawEmail));
	}

	/**
	 * Verifies a code inside a transaction that always commits attempt state.
	 * Failures are returned, not thrown, so the controller can map them after
	 * commit (throwing would roll back the attempt counter).
	 */
	@Transactional
	public Verification verifyCode(String rawEmail, String code, HttpServletRequest request) {
		String normalized = emailNormalizer.normalize(rawEmail);
		String ipHash = ipHasher.hash(request);
		Instant now = clock.instant();

		Optional<OtpChallenge> maybeChallenge = challenges.findActiveForUpdate(normalized);
		if (maybeChallenge.isEmpty()) {
			return Verification.failed(Verification.Failure.INVALID_CODE);
		}
		OtpChallenge challenge = maybeChallenge.get();

		if (challenge.getExpiresAt().isBefore(now)) {
			challenge.consume(now);
			securityEvents.log("OTP_EXPIRED", null, ipHash, Map.of("email", mask(rawEmail)));
			return Verification.failed(Verification.Failure.INVALID_CODE);
		}

		boolean matches = hashingService.constantTimeEquals(hashCode(normalized, code), challenge.getCodeHash());
		if (!matches) {
			challenge.incrementAttempts();
			if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
				challenge.consume(now);
				securityEvents.log("OTP_MAX_ATTEMPTS", null, ipHash, Map.of("email", mask(rawEmail)));
			}
			return Verification.failed(Verification.Failure.INVALID_CODE);
		}

		challenge.consume(now);
		User user = userService.findOrCreateVerifiedUser(
				rawEmail.trim(), ipHash, IdentityProvider.EMAIL_OTP, normalized, null, null);
		if (!user.isActive()) {
			securityEvents.log("SUSPENDED_LOGIN_BLOCKED", user.getId(), ipHash, Map.of("flow", "otp"));
			return Verification.failed(Verification.Failure.SUSPENDED);
		}
		SessionService.CreatedSession session = sessionService.create(user, ipHash, request.getHeader("User-Agent"));
		return Verification.succeeded(new AuthResult(user, session.token(), session.expiresAt()));
	}

	/** Outcome of a verification attempt. */
	public record Verification(AuthResult authResult, Failure failure) {

		public enum Failure {
			INVALID_CODE,
			SUSPENDED
		}

		static Verification succeeded(AuthResult result) {
			return new Verification(result, null);
		}

		static Verification failed(Failure failure) {
			return new Verification(null, failure);
		}

		public boolean isSuccess() {
			return authResult != null;
		}
	}

	private boolean isDevFixedOtpActive() {
		String fixed = properties.auth().devFixedOtp();
		return fixed != null && !fixed.isBlank()
				&& !environment.acceptsProfiles(Profiles.of("prod"));
	}

	private String hashCode(String normalizedEmail, String code) {
		return hashingService.hmacSha256Hex(
				properties.security().otpPepper(),
				normalizedEmail + ":" + code);
	}

	static String mask(String email) {
		int at = email == null ? -1 : email.indexOf('@');
		if (at <= 1) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(at);
	}
}
