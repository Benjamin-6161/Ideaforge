package com.app.ideaforge.ideas;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.credits.CreditLedgerEntry;
import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.ideas.dto.RevalidationInfo;
import com.app.ideaforge.ideas.dto.RevalidationResponse;

/**
 * Starts deep re-validation runs. Charges 0.5 credits (REVALIDATE) with
 * idempotency; the pipeline runs after commit and refunds on failure.
 */
@Service
public class RevalidationService {

	private final IdeaRepository ideas;
	private final ValidationRunRepository runs;
	private final CreditService creditService;
	private final DeepValidationPipeline pipeline;

	public RevalidationService(
			IdeaRepository ideas,
			ValidationRunRepository runs,
			CreditService creditService,
			DeepValidationPipeline pipeline) {
		this.ideas = ideas;
		this.runs = runs;
		this.creditService = creditService;
		this.pipeline = pipeline;
	}

	@Transactional
	public RevalidationResponse create(UUID userId, UUID ideaId, String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw ApiException.badRequest(ErrorCodes.IDEMPOTENCY_KEY_REQUIRED,
					"An Idempotency-Key header is required for this endpoint.");
		}
		if (idempotencyKey.length() > 100) {
			throw ApiException.badRequest(ErrorCodes.VALIDATION_ERROR, "Idempotency-Key is too long.");
		}
		Idea idea = ideas.findByIdAndUserIdAndDeletedAtIsNull(ideaId, userId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Idea not found."));
		if (idea.getStatus() == IdeaStatus.FINALIZED) {
			throw ApiException.conflict(ErrorCodes.IDEA_LOCKED,
					"Finalized ideas are read-only; re-validation is not available.");
		}
		if (idea.getCurrentVersionId() == null) {
			throw ApiException.conflict(ErrorCodes.CONFLICTING_JOB, "The idea has no content version yet.");
		}

		Optional<CreditLedgerEntry> previousSpend = creditService.findByIdempotencyKey(userId, idempotencyKey);
		if (previousSpend.isPresent()) {
			Optional<ValidationRun> previousRun = runs.findBySpendLedgerId(previousSpend.get().getId());
			if (previousRun.isPresent()) {
				return new RevalidationResponse(previousRun.get().getId(), previousRun.get().getStatus().name());
			}
		}

		if (runs.existsByIdeaIdAndStatus(ideaId, ValidationRun.Status.RUNNING)) {
			throw ApiException.conflict(ErrorCodes.CONFLICTING_JOB,
					"A re-validation is already running for this idea.");
		}

		CreditLedgerEntry spend = creditService.spend(
				userId, "REVALIDATE", "validation_run", ideaId, idempotencyKey);
		Optional<ValidationRun> raced = runs.findBySpendLedgerId(spend.getId());
		if (raced.isPresent()) {
			return new RevalidationResponse(raced.get().getId(), raced.get().getStatus().name());
		}

		ValidationRun run = runs.save(
				new ValidationRun(ideaId, userId, idea.getCurrentVersionId(), spend.getId()));
		scheduleAfterCommit(run.getId());
		return new RevalidationResponse(run.getId(), run.getStatus().name());
	}

	@Transactional(readOnly = true)
	public Optional<RevalidationInfo> latestInfo(UUID ideaId) {
		return runs.findFirstByIdeaIdOrderByCreatedAtDesc(ideaId)
				.map(run -> new RevalidationInfo(
						run.getStatus().name(),
						run.getErrorCode(),
						run.getCreatedAt(),
						run.getCompletedAt()));
	}

	private void scheduleAfterCommit(UUID runId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			pipeline.runAsync(runId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				pipeline.runAsync(runId);
			}
		});
	}

	/** Used by the HTTP layer to map conflicts consistently. */
	static HttpStatus conflictStatus() {
		return HttpStatus.CONFLICT;
	}
}
