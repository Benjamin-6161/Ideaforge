package com.app.ideaforge.auth;

import java.util.UUID;

import com.app.ideaforge.IntegrationTestBase;
import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.security.HashingService;
import com.app.ideaforge.user.EmailNormalizer;
import com.app.ideaforge.user.User;
import com.app.ideaforge.user.UserRepository;
import com.app.ideaforge.user.UserStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends IntegrationTestBase {

	@MockitoBean
	EmailSender emailSender;

	@MockitoBean
	GoogleTokenVerifier googleTokenVerifier;

	@Autowired
	HashingService hashingService;

	@Autowired
	OtpChallengeRepository challenges;

	@Autowired
	UserRepository users;

	@Autowired
	EmailNormalizer emailNormalizer;

	@Autowired
	AppProperties properties;

	@Test
	void otpHappyPathCreatesUserAndSession() throws Exception {
		String email = uniqueEmail();
		Cookie cookie = login(email);

		mockMvc.perform(get("/api/me").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("USER"));

		assertThat(users.findByEmailNormalized(emailNormalizer.normalize(email))).isPresent();
		verify(emailSender).sendOtp(eq(email), any(), anyInt());
	}

	@Test
	void otpRequestAlwaysReturns202ForDisposableEmailButDoesNotSend() throws Exception {
		MockHttpServletResponse response = requestOtp("someone@mailinator.com");
		assertThat(response.getStatus()).isEqualTo(202);
		verify(emailSender, never()).sendOtp(any(), any(), anyInt());
	}

	@Test
	void wrongCodeAndUnknownEmailProduceTheSameError() throws Exception {
		String known = uniqueEmail();
		requestOtp(known);

		String knownProblem = verifyExpectingError(known, "999999");
		String unknownProblem = verifyExpectingError(uniqueEmail(), "999999");

		assertThat(knownProblem).contains("INVALID_CODE");
		assertThat(unknownProblem).contains("INVALID_CODE");
	}

	@Test
	void fiveWrongAttemptsInvalidateTheChallenge() throws Exception {
		String email = uniqueEmail();
		requestOtp(email);

		for (int i = 0; i < 5; i++) {
			verifyExpectingError(email, "111111");
		}
		// The correct code must now be rejected: the challenge is invalidated.
		MvcResult result = mockMvc.perform(csrfJson(post("/api/auth/otp/verify"),
				"""
				{"email":"%s","code":"000000"}
				""".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).contains("INVALID_CODE");
	}

	@Test
	void expiredChallengeIsRejected() throws Exception {
		String email = uniqueEmail();
		String normalized = emailNormalizer.normalize(email);
		String codeHash = hashingService.hmacSha256Hex(
				properties.security().otpPepper(), normalized + ":000000");
		challenges.save(new OtpChallenge(
				normalized,
				codeHash,
				5,
				java.time.Instant.now().minusSeconds(60),
				null));

		verifyExpectingError(email, "000000");
	}

	@Test
	void suspendedUserCannotLogin() throws Exception {
		String email = uniqueEmail();
		login(email);

		User user = users.findByEmailNormalized(emailNormalizer.normalize(email)).orElseThrow();
		user.setStatus(UserStatus.SUSPENDED);
		users.save(user);

		// New OTP request + verify: blocked after verification with a clear error.
		requestOtp(email);

		MvcResult result = mockMvc.perform(csrfJson(post("/api/auth/otp/verify"),
				"""
				{"email":"%s","code":"000000"}
				""".formatted(email)))
				.andExpect(status().isForbidden())
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).contains("ACCOUNT_SUSPENDED");
	}

	@Test
	void signupThrottleBlocksTheFourthAccountFromOneIp() throws Exception {
		for (int i = 0; i < 3; i++) {
			login(uniqueEmail());
		}
		String fourth = uniqueEmail();
		requestOtp(fourth);
		MvcResult result = mockMvc.perform(csrfJson(post("/api/auth/otp/verify"),
				"""
				{"email":"%s","code":"000000"}
				""".formatted(fourth)))
				.andExpect(status().isForbidden())
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).contains("SIGNUP_BLOCKED");
	}

	@Test
	void googleLoginCreatesSessionAndUser() throws Exception {
		String email = uniqueEmail();
		org.mockito.Mockito.when(googleTokenVerifier.verify("id-token-1"))
				.thenReturn(new GoogleTokenVerifier.GoogleAccount(
						"google-sub-" + UUID.randomUUID(), email, true, "Test Person", null));

		MvcResult result = mockMvc.perform(csrfJson(post("/api/auth/google"),
				"""
				{"idToken":"id-token-1"}
				"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andReturn();

		Cookie cookie = sessionCookie(result.getResponse());
		mockMvc.perform(get("/api/me").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Test Person"));
	}

	@Test
	void logoutRevokesSession() throws Exception {
		Cookie cookie = login(uniqueEmail());

		mockMvc.perform(withIp(post("/api/auth/logout")).cookie(cookie))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/me").cookie(cookie))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void csrfBootstrapReturnsTokenAndSetsCookie() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
				.andReturn();
		assertThat(result.getResponse().getHeader("Set-Cookie")).contains("XSRF-TOKEN=");
		assertThat(result.getResponse().getContentAsString()).contains("\"token\"");
	}

	@Test
	void otpEmailRateLimitDropsRequestsAfterFivePerHour() throws Exception {
		String email = uniqueEmail();
		for (int i = 0; i < 6; i++) {
			requestOtp(email);
		}

		verify(emailSender, times(5)).sendOtp(eq(email), any(), anyInt());
		long challengeCount = challenges.findAll().stream()
				.filter(challenge -> challenge.getEmailNormalized()
						.equals(emailNormalizer.normalize(email)))
				.count();
		assertThat(challengeCount).isEqualTo(5);
	}

	@Test
	void sessionCookieIsHttpOnlyAndLax() throws Exception {
		String email = uniqueEmail();
		requestOtp(email);
		MockHttpServletResponse response = mockMvc.perform(csrfJson(post("/api/auth/otp/verify"),
				"""
				{"email":"%s","code":"000000"}
				""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse();
		String setCookie = response.getHeader("Set-Cookie");
		assertThat(setCookie).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/");
	}

	private String verifyExpectingError(String email, String code) throws Exception {
		MvcResult result = mockMvc.perform(csrfJson(post("/api/auth/otp/verify"),
				"""
				{"email":"%s","code":"%s"}
				""".formatted(email, code)))
				.andExpect(status().isUnauthorized())
				.andReturn();
		return result.getResponse().getContentAsString();
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder csrfJson(
			org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String content) {
		return withIp(builder).contentType(MediaType.APPLICATION_JSON).content(content);
	}

	private String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}
}
