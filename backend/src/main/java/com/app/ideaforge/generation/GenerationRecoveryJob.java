package com.app.ideaforge.generation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.app.ideaforge.credits.CreditService;

/**
 * On startup, any PENDING/RUNNING generation is stale (the process restarted):
 * mark it FAILED and refund its spend exactly once.
 */
@Component
public class GenerationRecoveryJob implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(GenerationRecoveryJob.class);

	private final GenerationRunRepository runs;
	private final CreditService creditService;
	private final TransactionTemplate transactions;

	public GenerationRecoveryJob(
			GenerationRunRepository runs,
			CreditService creditService,
			PlatformTransactionManager transactionManager) {
		this.runs = runs;
		this.creditService = creditService;
		this.transactions = new TransactionTemplate(transactionManager);
	}

	@Override
	public void run(ApplicationArguments args) {
		List<GenerationRun> stale = runs.findByStatusIn(List.of(RunStatus.PENDING, RunStatus.RUNNING));
		for (GenerationRun staleRun : stale) {
			transactions.executeWithoutResult(status -> runs.findById(staleRun.getId()).ifPresent(run -> {
				if (run.getStatus() == RunStatus.SUCCEEDED || run.getStatus() == RunStatus.FAILED) {
					return;
				}
				if (run.getRefundLedgerId() == null && run.getSpendLedgerId() != null) {
					creditService.refund(run.getSpendLedgerId(), "Generation interrupted by restart")
							.ifPresent(refund -> run.markRefunded(refund.getId()));
				}
				run.fail("APP_RESTART");
				log.info("Recovered stale generation run {}", run.getId());
			}));
		}
	}
}
