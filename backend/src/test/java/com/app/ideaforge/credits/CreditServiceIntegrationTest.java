package com.app.ideaforge.credits;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.app.ideaforge.IntegrationTestBase;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.security.SecurityEventRepository;
import com.app.ideaforge.user.EmailNormalizer;
import com.app.ideaforge.user.User;
import com.app.ideaforge.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditServiceIntegrationTest extends IntegrationTestBase {

	@Autowired
	CreditService creditService;

	@Autowired
	CreditLedgerRepository ledger;

	@Autowired
	CreditReconciliationJob reconciliationJob;

	@Autowired
	CreditWalletRepository wallets;

	@Autowired
	UserRepository users;

	@Autowired
	EmailNormalizer emailNormalizer;

	@Autowired
	SecurityEventRepository securityEvents;

	@Test
	void signupGrantIsAppliedOnceAndMatchesLedger() throws Exception {
		String email = "credits-signup-" + UUID.randomUUID() + "@example.com";
		login(email);

		User user = users.findByEmailNormalized(emailNormalizer.normalize(email)).orElseThrow();
		UUID userId = user.getId();

		assertThat(creditService.balanceUnits(userId)).isEqualTo(50);
		assertThat(creditService.ledgerSum(userId)).isEqualTo(50);
		assertThat(ledger.findByUserIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.PageRequest.of(0, 10)))
				.extracting(CreditLedgerEntry::getEntryType)
				.containsExactly(LedgerEntryType.GRANT_SIGNUP);
	}

	@Test
	void spendThenRefundRestoresBalanceAndRefundsOnlyOnce() {
		UUID userId = newUser();
		creditService.grantAdmin(userId, 100, "test-grant-" + userId, "test");
		CreditLedgerEntry spend = creditService.spend(
				userId, "GENERATE_IDEAS", "generation_run", UUID.randomUUID(), null);

		assertThat(creditService.balanceUnits(userId)).isEqualTo(80);
		assertThat(spend.getAmountUnits()).isEqualTo(-20);

		var firstRefund = creditService.refund(spend.getId(), "test refund");
		assertThat(firstRefund).isPresent();
		assertThat(creditService.balanceUnits(userId)).isEqualTo(100);

		var secondRefund = creditService.refund(spend.getId(), "duplicate refund");
		assertThat(secondRefund).isEmpty();
		assertThat(creditService.balanceUnits(userId)).isEqualTo(100);
		assertThat(creditService.ledgerSum(userId)).isEqualTo(100);
	}

	@Test
	void insufficientCreditsRejectsWith402AndNoSideEffects() {
		UUID userId = newUser();
		creditService.grantAdmin(userId, 10, "test-grant-" + userId, "test");

		assertThatThrownBy(() -> creditService.spend(
				userId, "GENERATE_IDEAS", "generation_run", UUID.randomUUID(), null))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INSUFFICIENT_CREDITS"));

		assertThat(creditService.balanceUnits(userId)).isEqualTo(10);
		assertThat(creditService.ledgerSum(userId)).isEqualTo(10);
	}

	@Test
	void idempotencyKeyReturnsOriginalSpend() {
		UUID userId = newUser();
		creditService.grantAdmin(userId, 100, "test-grant-" + userId, "test");

		CreditLedgerEntry first = creditService.spend(userId, "WORK_MESSAGE", "chat", UUID.randomUUID(), "key-1");
		CreditLedgerEntry second = creditService.spend(userId, "WORK_MESSAGE", "chat", UUID.randomUUID(), "key-1");

		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(creditService.balanceUnits(userId)).isEqualTo(98);
	}

	@Test
	void concurrentSpendsNeverOverdraw() throws Exception {
		UUID userId = newUser();
		creditService.grantAdmin(userId, 100, "test-grant-" + userId, "test");

		int threads = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			tasks.add(() -> {
				try {
					creditService.spend(userId, "GENERATE_IDEAS", "generation_run", UUID.randomUUID(), null);
					return true;
				} catch (ApiException ex) {
					return false;
				}
			});
		}
		int successes = 0;
		for (Future<Boolean> future : executor.invokeAll(tasks)) {
			if (future.get()) {
				successes++;
			}
		}
		executor.shutdown();

		assertThat(successes).isLessThanOrEqualTo(5);
		long balance = creditService.balanceUnits(userId);
		assertThat(balance).isEqualTo(100 - successes * 20L);
		assertThat(creditService.ledgerSum(userId)).isEqualTo(balance);
	}

	@Test
	void reconciliationFlagsMismatchedWallets() {
		User user = new User(
				"recon-" + UUID.randomUUID() + "@example.com",
				"recon-" + UUID.randomUUID() + "@example.com",
				null);
		user.setEmailVerifiedAt(java.time.Instant.now());
		UUID userId = users.save(user).getId();
		wallets.save(new CreditWallet(userId, 7)); // balance with no ledger rows

		reconciliationJob.reconcile();

		assertThat(securityEvents.existsByTypeAndUserId("CREDIT_RECONCILIATION_MISMATCH", userId)).isTrue();
	}

	private UUID newUser() {
		User user = new User(
				"credit-test-" + UUID.randomUUID() + "@example.com",
				"credit-test-" + UUID.randomUUID() + "@example.com",
				null);
		user.setEmailVerifiedAt(java.time.Instant.now());
		return users.save(user).getId();
	}
}
