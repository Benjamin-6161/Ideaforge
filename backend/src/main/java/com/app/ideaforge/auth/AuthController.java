package com.app.ideaforge.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.ideaforge.auth.dto.CsrfResponse;
import com.app.ideaforge.auth.dto.GoogleAuthRequest;
import com.app.ideaforge.auth.dto.MeResponse;
import com.app.ideaforge.auth.dto.OtpRequest;
import com.app.ideaforge.auth.dto.OtpVerifyRequest;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.security.AuthUser;
import com.app.ideaforge.user.User;
import com.app.ideaforge.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api")
public class AuthController {

	private final OtpService otpService;
	private final GoogleAuthService googleAuthService;
	private final SessionService sessionService;
	private final SessionCookieService cookieService;
	private final UserService userService;

	public AuthController(
			OtpService otpService,
			GoogleAuthService googleAuthService,
			SessionService sessionService,
			SessionCookieService cookieService,
			UserService userService) {
		this.otpService = otpService;
		this.googleAuthService = googleAuthService;
		this.sessionService = sessionService;
		this.cookieService = cookieService;
		this.userService = userService;
	}

	/** Always 202, regardless of whether the address is usable (no enumeration). */
	@PostMapping("/auth/otp/request")
	public ResponseEntity<Void> requestOtp(@Valid @RequestBody OtpRequest body, HttpServletRequest request) {
		otpService.requestCode(body.email(), body.turnstileToken(), request);
		return ResponseEntity.accepted().build();
	}

	/** Resolves the CSRF token so the SPA can echo it in X-XSRF-TOKEN. */
	@GetMapping("/auth/csrf")
	public CsrfResponse csrf(CsrfToken csrfToken) {
		return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName());
	}

	@PostMapping("/auth/otp/verify")
	public ResponseEntity<MeResponse> verifyOtp(
			@Valid @RequestBody OtpVerifyRequest body,
			HttpServletRequest request,
			HttpServletResponse response) {
		OtpService.Verification verification = otpService.verifyCode(body.email(), body.code(), request);
		if (!verification.isSuccess()) {
			if (verification.failure() == OtpService.Verification.Failure.SUSPENDED) {
				throw ApiException.forbidden(ErrorCodes.ACCOUNT_SUSPENDED, "This account is suspended.");
			}
			throw ApiException.unauthorized(ErrorCodes.INVALID_CODE, "Invalid or expired code.");
		}
		AuthResult result = verification.authResult();
		setSessionCookie(response, result);
		return ResponseEntity.ok(MeResponse.from(result.user()));
	}

	@PostMapping("/auth/google")
	public ResponseEntity<MeResponse> google(
			@Valid @RequestBody GoogleAuthRequest body,
			HttpServletRequest request,
			HttpServletResponse response) {
		AuthResult result = googleAuthService.login(body.idToken(), body.turnstileToken(), request);
		setSessionCookie(response, result);
		return ResponseEntity.ok(MeResponse.from(result.user()));
	}

	@PostMapping("/auth/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		sessionService.revoke(cookieService.readToken(request));
		cookieService.clearCookie(response);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal AuthUser authUser) {
		if (authUser == null) {
			throw ApiException.unauthorized(ErrorCodes.UNAUTHORIZED, "Authentication required.");
		}
		User user = userService.findById(authUser.id())
				.orElseThrow(() -> ApiException.unauthorized(ErrorCodes.UNAUTHORIZED, "Authentication required."));
		return MeResponse.from(user);
	}

	private void setSessionCookie(HttpServletResponse response, AuthResult result) {
		cookieService.writeCookie(response, result.token(),
				Duration.between(Instant.now(), result.expiresAt()));
	}
}
