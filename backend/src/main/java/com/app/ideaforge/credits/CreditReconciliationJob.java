package com.app.ideaforge.credits;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.security.SecurityEventService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Nightly invariant check: every wallet balance must equal the sum of its
 * ledger rows. Mismatches are logged as security events and counted.
 */
@Component
public class CreditReconciliationJob {

	private static final Logger log = LoggerFactory.getLogger(CreditReconciliationJob.class);

	private final CreditWalletRepository wallets;
	private final CreditLedgerRepository ledger;
	private final SecurityEventService securityEvents;
	private final Counter mismatchCounter;

	public CreditReconciliationJob(
			CreditWalletRepository wallets,
			CreditLedgerRepository ledger,
			SecurityEventService securityEvents,
			MeterRegistry meterRegistry) {
		this.wallets = wallets;
		this.ledger = ledger;
		this.securityEvents = securityEvents;
		this.mismatchCounter = Counter.builder("ideaforge.credits.reconciliation.mismatches")
				.description("Wallet balances that differ from the sum of their ledger rows")
				.register(meterRegistry);
	}

	@Scheduled(cron = "${app.credits.reconciliation-cron}")
	@Transactional(readOnly = true)
	public void reconcile() {
		Map<UUID, Long> sums = new HashMap<>();
		for (Object[] row : ledger.sumAmountsPerUser()) {
			sums.put((UUID) row[0], ((Number) row[1]).longValue());
		}
		int mismatches = 0;
		for (CreditWallet wallet : wallets.findAll()) {
			long expected = sums.getOrDefault(wallet.getUserId(), 0L);
			if (wallet.getBalanceUnits() != expected) {
				mismatches++;
				mismatchCounter.increment();
				securityEvents.log("CREDIT_RECONCILIATION_MISMATCH", wallet.getUserId(), null,
						Map.of("walletUnits", wallet.getBalanceUnits(), "ledgerUnits", expected));
			}
		}
		if (mismatches > 0) {
			log.error("Credit reconciliation found {} mismatches", mismatches);
		} else {
			log.info("Credit reconciliation passed for {} wallets", wallets.count());
		}
	}
}
