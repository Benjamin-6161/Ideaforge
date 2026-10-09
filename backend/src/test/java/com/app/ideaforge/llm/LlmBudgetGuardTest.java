package com.app.ideaforge.llm;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmBudgetGuardTest {

	private final AiUsageLogRepository repository = mock(AiUsageLogRepository.class);
	private final Clock clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void allowsSpendingBelowBudget() {
		when(repository.sumEstimatedCostMicroUsdSince(any())).thenReturn(1_000_000L); // $1
		LlmBudgetGuard guard = guard(new BigDecimal("10"));
		assertThatCode(guard::assertWithinBudget).doesNotThrowAnyException();
	}

	@Test
	void rejectsWhenDailyBudgetExceeded() {
		when(repository.sumEstimatedCostMicroUsdSince(any())).thenReturn(10_000_000L); // $10 of $10
		LlmBudgetGuard guard = guard(new BigDecimal("10"));
		assertThatThrownBy(guard::assertWithinBudget)
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> {
					ApiException api = (ApiException) ex;
					assertThat(api.getCode()).isEqualTo(ErrorCodes.TEMPORARILY_UNAVAILABLE);
					assertThat(api.getStatus().value()).isEqualTo(503);
				});
	}

	@Test
	void zeroBudgetMeansUnlimited() {
		LlmBudgetGuard guard = guard(BigDecimal.ZERO);
		assertThatCode(guard::assertWithinBudget).doesNotThrowAnyException();
	}

	private LlmBudgetGuard guard(BigDecimal dailyBudget) {
		AppProperties properties = new AppProperties(
				null, null, null, null, null, null, null, null,
				new AppProperties.Llm(dailyBudget, java.util.Map.of(), java.util.Map.of(),
						new AppProperties.Llm.Pricing(BigDecimal.ZERO, BigDecimal.ZERO, java.util.Map.of(), java.util.Map.of())),
				null, null);
		return new LlmBudgetGuard(repository, properties, clock);
	}
}
