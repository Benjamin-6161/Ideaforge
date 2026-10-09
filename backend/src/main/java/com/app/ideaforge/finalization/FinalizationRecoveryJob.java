package com.app.ideaforge.finalization;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.app.ideaforge.credits.CreditService;

/** Startup recovery for finalizations interrupted by a restart. */
@Component
public class FinalizationRecoveryJob implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(FinalizationRecoveryJob.class);

	private final FinalizationRepository finalizations;
	private final CreditService creditService;
	private final TransactionTemplate transactions;

	public FinalizationRecoveryJob(
			FinalizationRepository finalizations,
			CreditService creditService,
			PlatformTransactionManager transactionManager) {
		this.finalizations = finalizations;
		this.creditService = creditService;
		this.transactions = new TransactionTemplate(transactionManager);
	}

	@Override
	public void run(ApplicationArguments args) {
		List<Finalization> stale = finalizations.findByStatusIn(List.of(Finalization.Status.RUNNING));
		for (Finalization staleRun : stale) {
			transactions.executeWithoutResult(status -> finalizations.findById(staleRun.getId())
					.ifPresent(finalization -> {
						if (finalization.getStatus() != Finalization.Status.RUNNING) {
							return;
						}
						finalization.fail();
						if (finalization.getRefundLedgerId() == null && finalization.getSpendLedgerId() != null) {
							creditService.refund(finalization.getSpendLedgerId(),
											"Finalization interrupted by restart")
									.ifPresent(refund -> finalization.markRefunded(refund.getId()));
						}
						log.info("Recovered stale finalization {}", finalization.getId());
					}));
		}
	}
}
