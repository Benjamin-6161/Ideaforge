package com.app.ideaforge.admin;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.credits.CreditLedgerRepository;
import com.app.ideaforge.llm.AiUsageLogRepository;
import com.app.ideaforge.user.UserRepository;

/** Aggregates for the admin-only usage dashboard. */
@Service
public class AdminUsageService {

	private final AiUsageLogRepository usageLogs;
	private final UserRepository users;
	private final CreditLedgerRepository ledger;
	private final AppProperties properties;
	private final Clock clock;

	public AdminUsageService(
			AiUsageLogRepository usageLogs,
			UserRepository users,
			CreditLedgerRepository ledger,
			AppProperties properties,
			Clock clock) {
		this.usageLogs = usageLogs;
		this.users = users;
		this.ledger = ledger;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public AdminUsageResponse usage(int days) {
		int window = Math.max(1, Math.min(days, 90));
		Instant since = clock.instant().minus(window, ChronoUnit.DAYS);

		List<AdminUsageResponse.DailySpend> daily = usageLogs.dailySpendSince(since).stream()
				.map(row -> new AdminUsageResponse.DailySpend(
						row[0].toString().substring(0, 10),
						((Number) row[1]).longValue(),
						((Number) row[2]).longValue()))
				.toList();

		List<AdminUsageResponse.PurposeModelUsage> byPurposeModel = usageLogs.usageSince(since).stream()
				.map(row -> new AdminUsageResponse.PurposeModelUsage(
						row[0].toString(),
						row[1].toString(),
						row[2].toString(),
						((Number) row[3]).longValue(),
						((Number) row[4]).longValue(),
						((Number) row[5]).longValue(),
						((Number) row[6]).longValue()))
				.toList();

		long signups = users.countByCreatedAtAfter(since);
		long totalUsers = users.count();

		long granted = 0;
		long spent = 0;
		long refunded = 0;
		for (Object[] row : ledger.sumsByTypeSince(since)) {
			String type = row[0].toString();
			long sum = ((Number) row[1]).longValue();
			switch (type) {
				case "SPEND" -> spent += -sum;
				case "REFUND" -> refunded += sum;
				case "GRANT_SIGNUP", "GRANT_ADMIN", "PURCHASE", "ADJUSTMENT" -> granted += sum;
				default -> {
					// ignore unknown types
				}
			}
		}

		BigDecimal budget = properties.llm().dailyBudgetUsd();
		long budgetMicroUsd = budget == null
				? 0
				: budget.multiply(BigDecimal.valueOf(1_000_000)).longValue();

		return new AdminUsageResponse(
				window,
				daily,
				byPurposeModel,
				new AdminUsageResponse.Signups(signups, totalUsers),
				new AdminUsageResponse.Credits(granted, spent, refunded),
				budgetMicroUsd);
	}

	public record AdminUsageResponse(
			int windowDays,
			List<DailySpend> daily,
			List<PurposeModelUsage> byPurposeModel,
			Signups signups,
			Credits credits,
			long dailyLlmBudgetMicroUsd) {

		public record DailySpend(String day, long calls, long estimatedCostMicroUsd) {
		}

		public record PurposeModelUsage(
				String purpose,
				String provider,
				String model,
				long calls,
				long tokensIn,
				long tokensOut,
				long estimatedCostMicroUsd) {
		}

		public record Signups(long inWindow, long total) {
		}

		public record Credits(long grantedUnits, long spentUnits, long refundedUnits) {
		}
	}
}
