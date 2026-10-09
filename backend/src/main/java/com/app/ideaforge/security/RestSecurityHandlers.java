package com.app.ideaforge.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.app.ideaforge.common.error.ErrorCodes;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Writes RFC 7807 problem+json bodies for 401/403 produced by the filter chain. */
@Component
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public RestSecurityHandlers(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		write(response, HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Authentication required.");
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		write(response, HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Access denied.");
	}

	private void write(HttpServletResponse response, HttpStatus status, String code, String detail)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "about:blank");
		body.put("title", status.getReasonPhrase());
		body.put("status", status.value());
		body.put("detail", detail);
		body.put("code", code);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
