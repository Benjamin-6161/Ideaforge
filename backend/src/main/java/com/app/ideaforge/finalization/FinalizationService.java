package com.app.ideaforge.finalization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.credits.CreditLedgerEntry;
import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.finalization.dto.FinalizationResponse;
import com.app.ideaforge.ideas.Idea;
import com.app.ideaforge.ideas.IdeaRepository;
import com.app.ideaforge.ideas.IdeaStatus;

/**
 * Starts finalizations: 1 credit (FINALIZE), idempotent, one active per user,
 * and locked thereafter. The pipeline runs after commit.
 */
@Service
public class FinalizationService {

	private final IdeaRepository ideas;
	private final FinalizationRepository finalizations;
	private final CreditService creditService;
	private final FinalizePipeline pipeline;
	private final FinalizePrompts prompts;

	public FinalizationService(
			IdeaRepository ideas,
			FinalizationRepository finalizations,
			CreditService creditService,
			FinalizePipeline pipeline,
			FinalizePrompts prompts) {
		this.ideas = ideas;
		this.finalizations = finalizations;
		this.creditService = creditService;
		this.pipeline = pipeline;
		this.prompts = prompts;
	}

	@Transactional
	public FinalizationResponse create(UUID userId, UUID ideaId, String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw ApiException.badRequest(ErrorCodes.IDEMPOTENCY_KEY_REQUIRED,
					"An Idempotency-Key header is required for this endpoint.");
		}
		if (idempotencyKey.length() > 100) {
			throw ApiException.badRequest(ErrorCodes.VALIDATION_ERROR, "Idempotency-Key is too long.");
		}
		Idea idea = ideas.findByIdAndUserIdAndDeletedAtIsNull(ideaId, userId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Idea not found."));

		// Replays return the original finalization, even after the idea is locked.
		Optional<CreditLedgerEntry> previousSpend = creditService.findByIdempotencyKey(userId, idempotencyKey);
		if (previousSpend.isPresent()) {
			Optional<Finalization> previous = finalizations.findBySpendLedgerId(previousSpend.get().getId());
			if (previous.isPresent()) {
				return response(previous.get());
			}
		}

		if (idea.getStatus() == IdeaStatus.FINALIZED) {
			throw ApiException.conflict(ErrorCodes.IDEA_LOCKED, "This idea is already finalized.");
		}
		if (idea.getCurrentVersionId() == null) {
			throw ApiException.conflict(ErrorCodes.CONFLICTING_JOB, "The idea has no content version yet.");
		}

		Optional<Finalization> existing = finalizations.findByIdeaId(ideaId);
		if (existing.isPresent() && existing.get().getStatus() == Finalization.Status.RUNNING) {
			throw ApiException.conflict(ErrorCodes.CONFLICTING_JOB,
					"A finalization is already running for this idea.");
		}
		if (finalizations.existsRunningForUser(userId)) {
			throw ApiException.conflict(ErrorCodes.CONFLICTING_JOB,
					"Another finalization is already running.");
		}

		CreditLedgerEntry spend = creditService.spend(
				userId, "FINALIZE", "finalization", ideaId, idempotencyKey);
		Optional<Finalization> raced = finalizations.findBySpendLedgerId(spend.getId());
		if (raced.isPresent()) {
			return response(raced.get());
		}

		Finalization finalization = finalizations.save(new Finalization(
				ideaId,
				idea.getCurrentVersionId(),
				spend.getId(),
				prompts.version()));
		scheduleAfterCommit(finalization.getId());
		return response(finalization);
	}

	@Transactional(readOnly = true)
	public FinalizationResponse get(UUID userId, UUID ideaId) {
		ideas.findByIdAndUserIdAndDeletedAtIsNull(ideaId, userId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Idea not found."));
		return finalizations.findByIdeaId(ideaId)
				.map(this::response)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "This idea has no finalization."));
	}

	private FinalizationResponse response(Finalization finalization) {
		return new FinalizationResponse(
				finalization.getId(),
				finalization.getIdeaId(),
				finalization.getVersionId(),
				finalization.getStatus().name(),
				finalization.getStatus() == Finalization.Status.FAILED ? "FINALIZE_FAILED" : null,
				finalization.getOutput(),
				finalization.getBuildPromptMd(),
				finalization.getAgentsMd(),
				finalization.getPromptVersion(),
				finalization.getCreatedAt(),
				finalization.getCompletedAt());
	}

	private void scheduleAfterCommit(UUID finalizationId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			pipeline.runAsync(finalizationId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				pipeline.runAsync(finalizationId);
			}
		});
	}
}
