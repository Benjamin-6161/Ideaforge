package com.app.ideaforge.credits;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Public credit API. Costs come from the database (cached briefly), never from
 * client input or code constants.
 */
@Service
public class CreditService {

	private final CreditLedgerTransactions transactions;
	private final CreditCostRepository costs;
	private final CreditWalletRepository wallets;
	private final CreditLedgerRepository ledger;
	private final CreditPackageRepository packages;
	private final Cache<String, Integer> costCache;
	private final AppProperties properties;

	public CreditService(
			CreditLedgerTransactions transactions,
			CreditCostRepository costs,
			CreditWalletRepository wallets,
			CreditLedgerRepository ledger,
			CreditPackageRepository packages,
			AppProperties properties) {
		this.transactions = transactions;
		this.costs = costs;
		this.wallets = wallets;
		this.ledger = ledger;
		this.packages = packages;
		this.properties = properties;
		this.costCache = Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofSeconds(properties.credits().costsCacheSeconds()))
				.build();
	}

	public long balanceUnits(UUID userId) {
		Long balance = wallets.findBalance(userId);
		return balance == null ? 0 : balance;
	}

	public int costUnits(String action) {
		Integer cached = costCache.get(action, this::loadCost);
		if (cached == null) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "COST_NOT_CONFIGURED",
					"No active cost configured for action " + action + ".");
		}
		return cached;
	}

	private Integer loadCost(String action) {
		return costs.findById(action)
				.filter(CreditCost::isActive)
				.map(CreditCost::getCostUnits)
				.orElse(null);
	}

	public List<CreditCost> activeCosts() {
		return costs.findByActiveTrue();
	}

	/** Spends with idempotency: retries return the original ledger entry. */
	public CreditLedgerEntry spend(
			UUID userId,
			String action,
			String refType,
			UUID refId,
			String idempotencyKey) {
		int cost = costUnits(action);
		try {
			return transactions.spend(userId, cost, action, refType, refId, idempotencyKey, null);
		} catch (DataIntegrityViolationException ex) {
			if (idempotencyKey != null) {
				return ledger.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
						.orElseThrow(() -> ex);
			}
			throw ex;
		}
	}

	/** Refunds a spend at most once; empty when already refunded or unknown. */
	public Optional<CreditLedgerEntry> refund(UUID spendEntryId, String note) {
		return transactions.refund(spendEntryId, note);
	}

	public CreditLedgerEntry grantSignup(UUID userId) {
		return transactions.grant(
				userId,
				LedgerEntryType.GRANT_SIGNUP,
				properties.credits().signupGrantUnits(),
				"signup-grant",
				"Welcome credits");
	}

	public CreditLedgerEntry grantAdmin(UUID userId, long units, String idempotencyKey, String note) {
		return transactions.grant(userId, LedgerEntryType.GRANT_ADMIN, units, idempotencyKey, note);
	}

	public Page<CreditLedgerEntry> ledgerPage(UUID userId, int page, int size) {
		int safeSize = Math.max(1, Math.min(size, 50));
		return ledger.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(0, page), safeSize));
	}

	public List<CreditPackage> activePackages() {
		return packages.findByActiveTrueOrderBySortOrderAsc();
	}

	/** Used by the reconciliation job and tests. */
	public long ledgerSum(UUID userId) {
		return ledger.sumAmountsForUser(userId);
	}
}
