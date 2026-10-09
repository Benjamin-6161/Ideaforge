package com.app.ideaforge.credits;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;

/**
 * Transactional core of the credit ledger. Every wallet change and its ledger
 * row happen in one transaction; spends use an atomic conditional update.
 */
@Service
public class CreditLedgerTransactions {

	private final CreditWalletRepository wallets;
	private final CreditLedgerRepository ledger;
	private final Clock clock;

	public CreditLedgerTransactions(CreditWalletRepository wallets, CreditLedgerRepository ledger, Clock clock) {
		this.wallets = wallets;
		this.ledger = ledger;
		this.clock = clock;
	}

	@Transactional
	public CreditLedgerEntry spend(
			UUID userId,
			int costUnits,
			String action,
			String refType,
			UUID refId,
			String idempotencyKey,
			String note) {
		ensureWallet(userId);
		// Serialize spends per user, then re-check idempotency under the lock.
		wallets.lockByUserId(userId);
		if (idempotencyKey != null) {
			Optional<CreditLedgerEntry> existing = ledger.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
			if (existing.isPresent()) {
				return existing.get();
			}
		}
		int updated = wallets.debit(userId, costUnits, clock.instant());
		if (updated == 0) {
			throw new ApiException(HttpStatus.PAYMENT_REQUIRED, ErrorCodes.INSUFFICIENT_CREDITS,
					"Not enough credits for this action.");
		}
		long balanceAfter = balanceAfterOrZero(userId);
		return ledger.save(new CreditLedgerEntry(
				userId, LedgerEntryType.SPEND, -costUnits, balanceAfter,
				action, refType, refId, null, idempotencyKey, note));
	}

	/**
	 * Refunds a spend exactly once. Locks the spend row so concurrent refunds
	 * cannot double-credit; the unique index on `related_entry_id` is a second
	 * line of defense.
	 */
	@Transactional
	public Optional<CreditLedgerEntry> refund(UUID spendEntryId, String note) {
		Optional<CreditLedgerEntry> maybeSpend = ledger.findByIdForUpdate(spendEntryId);
		if (maybeSpend.isEmpty()) {
			return Optional.empty();
		}
		CreditLedgerEntry spend = maybeSpend.get();
		if (spend.getEntryType() != LedgerEntryType.SPEND) {
			return Optional.empty();
		}
		if (ledger.existsByRelatedEntryId(spendEntryId)) {
			return Optional.empty();
		}
		UUID userId = spend.getUserId();
		ensureWallet(userId);
		long amount = -spend.getAmountUnits();
		wallets.credit(userId, amount, clock.instant());
		long balanceAfter = balanceAfterOrZero(userId);
		CreditLedgerEntry refund = ledger.save(new CreditLedgerEntry(
				userId, LedgerEntryType.REFUND, amount, balanceAfter,
				spend.getAction(), spend.getRefType(), spend.getRefId(),
				spendEntryId, null, note));
		return Optional.of(refund);
	}

	@Transactional
	public CreditLedgerEntry grant(
			UUID userId,
			LedgerEntryType entryType,
			long amountUnits,
			String idempotencyKey,
			String note) {
		if (amountUnits <= 0) {
			throw new IllegalArgumentException("Grant amount must be positive");
		}
		ensureWallet(userId);
		wallets.lockByUserId(userId);
		if (idempotencyKey != null) {
			Optional<CreditLedgerEntry> existing = ledger.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
			if (existing.isPresent()) {
				return existing.get();
			}
		}
		wallets.credit(userId, amountUnits, clock.instant());
		long balanceAfter = balanceAfterOrZero(userId);
		return ledger.save(new CreditLedgerEntry(
				userId, entryType, amountUnits, balanceAfter,
				null, null, null, null, idempotencyKey, note));
	}

	private void ensureWallet(UUID userId) {
		wallets.ensureExists(userId, clock.instant());
	}

	private long balanceAfterOrZero(UUID userId) {
		Long balance = wallets.findBalance(userId);
		return balance == null ? 0 : balance;
	}
}
