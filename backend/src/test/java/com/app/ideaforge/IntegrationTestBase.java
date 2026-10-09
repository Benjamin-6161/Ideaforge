package com.app.ideaforge;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base for integration tests: real PostgreSQL via Testcontainers, MockMvc, and
 * helpers for the dev fixed OTP login flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

	@Autowired
	protected MockMvc mockMvc;

	private String clientIp;

	@BeforeEach
	void assignClientIp() {
		// Each test gets its own client IP (via X-Forwarded-For, trusted in test)
		// so per-IP rate limits and signup throttling never leak across tests.
		clientIp = "10." + (int) (Math.random() * 250 + 1) + "."
				+ (int) (Math.random() * 250 + 1) + "." + (int) (Math.random() * 250 + 1);
	}

	protected String clientIp() {
		return clientIp;
	}

	protected MockHttpServletRequestBuilder withIp(MockHttpServletRequestBuilder builder) {
		return builder.header("X-Forwarded-For", clientIp).with(SecurityMockMvcRequestPostProcessors.csrf());
	}

	protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) {
		try {
			return withIp(builder)
					.contentType(MediaType.APPLICATION_JSON)
					.content(new tools.jackson.databind.ObjectMapper().writeValueAsString(body));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	protected MockHttpServletRequestBuilder getJson(String uri) {
		return getJson(uri, null);
	}

	protected MockHttpServletRequestBuilder getJson(String uri, Cookie cookie) {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(URI.create(uri));
		if (cookie != null) {
			builder = builder.cookie(cookie);
		}
		return builder;
	}

	/** Requests and verifies a dev-fixed OTP, returning the session cookie. */
	protected Cookie login(String email) throws Exception {
		requestOtp(email);
		MvcResult result = mockMvc.perform(withIp(MockMvcRequestBuilders.post("/api/auth/otp/verify"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","code":"000000"}
						""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();
		return sessionCookie(result.getResponse());
	}

	protected MockHttpServletResponse requestOtp(String email) throws Exception {
		MvcResult result = mockMvc.perform(json(
				MockMvcRequestBuilders.post("/api/auth/otp/request"),
				new OtpPayload(email, null)))
				.andExpect(status().isAccepted())
				.andReturn();
		return result.getResponse();
	}

	protected Cookie sessionCookie(MockHttpServletResponse response) {
		String setCookie = response.getHeader("Set-Cookie");
		if (setCookie == null) {
			throw new AssertionError("No Set-Cookie header in response");
		}
		String pair = setCookie.split(";", 2)[0];
		String[] parts = pair.split("=", 2);
		return new Cookie(parts[0], parts[1]);
	}

	public record OtpPayload(String email, String turnstileToken) {
	}

	protected String body(String text) {
		return new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
	}
}
