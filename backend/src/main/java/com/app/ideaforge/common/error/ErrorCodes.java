package com.app.ideaforge.common.error;

/** Stable machine-readable error codes returned in problem+json `code` properties. */
public final class ErrorCodes {

	private ErrorCodes() {
	}

	public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
	public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
	public static final String NOT_FOUND = "NOT_FOUND";
	public static final String UNAUTHORIZED = "UNAUTHORIZED";
	public static final String FORBIDDEN = "FORBIDDEN";
	public static final String INVALID_CODE = "INVALID_CODE";
	public static final String OTP_RATE_LIMITED = "OTP_RATE_LIMITED";
	public static final String EMAIL_NOT_ALLOWED = "EMAIL_NOT_ALLOWED";
	public static final String SIGNUP_BLOCKED = "SIGNUP_BLOCKED";
	public static final String ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";
	public static final String INSUFFICIENT_CREDITS = "INSUFFICIENT_CREDITS";
	public static final String IDEMPOTENCY_CONFLICT = "IDEMPOTENCY_CONFLICT";
	public static final String CONFLICTING_JOB = "CONFLICTING_JOB";
	public static final String DAILY_LIMIT_REACHED = "DAILY_LIMIT_REACHED";
	public static final String IDEMPOTENCY_KEY_REQUIRED = "IDEMPOTENCY_KEY_REQUIRED";
	public static final String IDEA_LOCKED = "IDEA_LOCKED";
	public static final String MESSAGE_LIMIT_REACHED = "MESSAGE_LIMIT_REACHED";
	public static final String INJECTION_DETECTED = "INJECTION_DETECTED";
	public static final String TEMPORARILY_UNAVAILABLE = "TEMPORARILY_UNAVAILABLE";
	public static final String UPSTREAM_UNAVAILABLE = "UPSTREAM_UNAVAILABLE";
	public static final String TURNSTILE_FAILED = "TURNSTILE_FAILED";
}
