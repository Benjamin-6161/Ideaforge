package com.app.ideaforge.common.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** All application configuration under the `app.*` prefix. Environment-overridable only. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
		String baseUrl,
		Cors cors,
		Security security,
		Auth auth,
		Email email,
		DisposableEmail disposableEmail,
		Credits credits,
		Limits limits,
		Llm llm,
		Research research,
		Seed seed) {

	public record Cors(List<String> allowedOrigins) {
	}

	public record Security(
			String sessionPepper,
			String otpPepper,
			String ipPepper,
			boolean trustProxy) {
	}

	public record Auth(
			String devFixedOtp,
			Otp otp,
			Session session,
			Signup signup,
			Google google,
			Turnstile turnstile) {

		public record Otp(
				int ttlMinutes,
				int maxAttempts,
				int resendCooldownSeconds,
				int maxRequestsPerEmailPerHour,
				int maxRequestsPerIpPerHour) {
		}

		public record Session(
				String cookieName,
				int idleDays,
				int absoluteDays,
				boolean cookieSecure,
				String cookieSameSite) {
		}

		public record Signup(int maxPerIpPerDay) {
		}

		public record Google(String clientId) {
		}

		public record Turnstile(
				boolean enabled,
				String verifyUrl,
				String secretKey,
				String siteKey) {
		}
	}

	public record Email(String from, String fromName) {
	}

	public record DisposableEmail(
			boolean refreshEnabled,
			String refreshUrl,
			String allowlist,
			boolean mxCheckEnabled) {
	}

	public record Credits(
			int costsCacheSeconds,
			String reconciliationCron,
			int signupGrantUnits) {
	}

	public record Limits(
			int generationsPerDay,
			int chatMessagesPerDay,
			int maxChatMessagesPerIdea,
			int maxActiveJobsPerUser) {
	}

	public record Llm(
			BigDecimal dailyBudgetUsd,
			Map<String, LlmProfile> profiles,
			Map<String, ProviderConfig> providers,
			Pricing pricing) {

		public record LlmProfile(
				String provider,
				String model,
				Double temperature,
				Integer maxOutputTokens,
				Integer timeoutSeconds,
				String baseUrl,
				String fallbackProfile) {
		}

		public record ProviderConfig(String apiKey, String baseUrl) {
		}

		public record Pricing(
				BigDecimal defaultInputPer1kMicroUsd,
				BigDecimal defaultOutputPer1kMicroUsd,
				Map<String, BigDecimal> inputPer1kMicroUsd,
				Map<String, BigDecimal> outputPer1kMicroUsd) {
		}
	}

	public record Research(
			Searxng searxng,
			HackerNews hackerNews,
			Trends trends,
			int searchCacheDays,
			PageFetcher pageFetcher) {

		public record Searxng(String baseUrl, boolean enabled) {
		}

		public record HackerNews(boolean enabled, String baseUrl) {
		}

		public record Trends(boolean enabled) {
		}

		public record PageFetcher(
				int timeoutSeconds,
				long maxBytes,
				int maxRedirects,
				int maxChars,
				boolean allowPrivateHosts) {
		}
	}

	public record Seed(boolean enabled, boolean testUsers, int testUserCreditsUnits) {
	}
}
