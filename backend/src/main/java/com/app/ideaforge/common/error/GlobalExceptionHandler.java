package com.app.ideaforge.common.error;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns exceptions into RFC 7807 problem+json responses with stable `code`
 * values. Stack traces and internals are never exposed.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ProblemDetail handleApiException(ApiException ex) {
		return problem(ex.getStatus(), ex.getCode(), ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Request validation failed");
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(this::fieldError)
				.toList();
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
		return problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Malformed request body");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleNoResource(NoResourceFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Resource not found");
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return problem(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Access denied");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Something went wrong");
	}

	private Map<String, String> fieldError(FieldError error) {
		return Map.of(
				"field", error.getField(),
				"message", error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage());
	}

	public static ProblemDetail problem(HttpStatus status, String code, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setProperty("code", code);
		return problem;
	}
}
