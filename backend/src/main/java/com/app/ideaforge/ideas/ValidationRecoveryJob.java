package com.app.ideaforge.ideas;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.app.ideaforge.credits.CreditService;

/** Startup recovery for re-validation runs interrupted by a restart. */
@Component
public class ValidationRecoveryJob implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ValidationRecoveryJob.class);

	private final ValidationRunRepository runs;
	private final CreditService creditService;
	private final TransactionTemplate transactions;

	public ValidationRecoveryJob(
			ValidationRunRepository runs,
			CreditService creditService,
			PlatformTransactionManager transactionManager) {
		this.runs = runs;
		this.creditService = creditService;
		this.transactions = new TransactionTemplate(transactionManager);
	}

	@Override
	public void run(ApplicationArguments args) {
		List<ValidationRun> stale = runs.findByStatusIn(List.of(ValidationRun.Status.RUNNING));
		for (ValidationRun staleRun : stale) {
			transactions.executeWithoutResult(status -> runs.findById(staleRun.getId()).ifPresent(run -> {
				if (run.getStatus() != ValidationRun.Status.RUNNING) {
					return;
				}
				if (run.getRefundLedgerId() == null && run.getSpendLedgerId() != null) {
					creditService.refund(run.getSpendLedgerId(), "Revalidation interrupted by restart")
							.ifPresent(refund -> run.markRefunded(refund.getId()));
				}
				run.fail("APP_RESTART");
				log.info("Recovered stale revalidation {}", run.getId());
			}));
		}
	}
}
