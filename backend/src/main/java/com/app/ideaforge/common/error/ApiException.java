package com.app.ideaforge.common.error;

import org.springframework.http.HttpStatus;

/**
 * Application exception carrying a stable error code and HTTP status.
 * Rendered as RFC 7807 problem+json by {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String code, String detail) {
		super(detail);
		this.status = status;
		this.code = code;
	}

	public static ApiException badRequest(String code, String detail) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, detail);
	}

	public static ApiException unauthorized(String code, String detail) {
		return new ApiException(HttpStatus.UNAUTHORIZED, code, detail);
	}

	public static ApiException forbidden(String code, String detail) {
		return new ApiException(HttpStatus.FORBIDDEN, code, detail);
	}

	public static ApiException notFound(String code, String detail) {
		return new ApiException(HttpStatus.NOT_FOUND, code, detail);
	}

	public static ApiException conflict(String code, String detail) {
		return new ApiException(HttpStatus.CONFLICT, code, detail);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
