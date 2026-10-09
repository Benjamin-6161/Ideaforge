package com.app.ideaforge.llm;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;

/**
 * Global daily LLM spend circuit breaker based on estimated costs in
 * ai_usage_logs. When exceeded, paid endpoints must fail with 503 and charge
 * nothing. The sum is cached briefly to keep the hot path cheap.
 */
@Component
public class LlmBudgetGuard {

	private static final Duration CACHE_TTL = Duration.ofSeconds(60);

	private final AiUsageLogRepository usageLogs;
	private final AppProperties properties;
	private final Clock clock;
	private final AtomicReference<CachedSpend> cache = new AtomicReference<>();

	private record CachedSpend(long microUsd, Instant computedAt) {
	}

	public LlmBudgetGuard(AiUsageLogRepository usageLogs, AppProperties properties, Clock clock) {
		this.usageLogs = usageLogs;
		this.properties = properties;
		this.clock = clock;
	}

	public void assertWithinBudget() {
		BigDecimal budgetUsd = properties.llm().dailyBudgetUsd();
		if (budgetUsd == null || budgetUsd.signum() <= 0) {
			return;
		}
		long budgetMicroUsd = budgetUsd.multiply(BigDecimal.valueOf(1_000_000)).longValue();
		Instant now = clock.instant();
		CachedSpend cached = cache.get();
		if (cached == null || cached.computedAt().isBefore(now.minus(CACHE_TTL))) {
			Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
			long spent = usageLogs.sumEstimatedCostMicroUsdSince(startOfDay);
			cached = new CachedSpend(spent, now);
			cache.set(cached);
		}
		if (cached.microUsd() >= budgetMicroUsd) {
			throw new ApiException(
					HttpStatus.SERVICE_UNAVAILABLE,
					ErrorCodes.TEMPORARILY_UNAVAILABLE,
					"AI capacity is temporarily unavailable. Please try again later.");
		}
	}
}
