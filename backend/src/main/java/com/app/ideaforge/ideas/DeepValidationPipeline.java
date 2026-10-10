package com.app.ideaforge.ideas;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.generation.GenerationPrompts;
import com.app.ideaforge.generation.ResearchBundle;
import com.app.ideaforge.generation.dto.ScoringResponse;
import com.app.ideaforge.ideas.dto.IdeaContent;
import com.app.ideaforge.llm.LlmErrorClassifier;
import com.app.ideaforge.llm.LlmException;
import com.app.ideaforge.llm.LlmGateway;
import com.app.ideaforge.llm.LlmPurpose;
import com.app.ideaforge.research.DeepResearchService;

import jakarta.validation.Validator;

/**
 * Deep re-validation: extensive research plus a scoring call against the
 * revalidated version. Failures refund the spend exactly once.
 */
@Service
public class DeepValidationPipeline {

	private static final Logger log = LoggerFactory.getLogger(DeepValidationPipeline.class);

	private final ValidationRunRepository runs;
	private final IdeaRepository ideas;
	private final IdeaVersionRepository versions;
	private final IdeaValidationRepository validations;
	private final DeepResearchService deepResearch;
	private final GenerationPrompts prompts;
	private final LlmGateway llmGateway;
	private final CreditService creditService;
	private final Validator validator;
	private final TransactionTemplate transactions;

	public DeepValidationPipeline(
			ValidationRunRepository runs,
			IdeaRepository ideas,
			IdeaVersionRepository versions,
			IdeaValidationRepository validations,
			DeepResearchService deepResearch,
			GenerationPrompts prompts,
			LlmGateway llmGateway,
			CreditService creditService,
			Validator validator,
			PlatformTransactionManager transactionManager) {
		this.runs = runs;
		this.ideas = ideas;
		this.versions = versions;
		this.validations = validations;
		this.deepResearch = deepResearch;
		this.prompts = prompts;
		this.llmGateway = llmGateway;
		this.creditService = creditService;
		this.validator = validator;
		this.transactions = new TransactionTemplate(transactionManager);
	}

	@Async
	public void runAsync(UUID runId) {
		try {
			execute(runId);
		} catch (Exception ex) {
			String code = errorCode(ex);
			log.warn("Revalidation {} failed with {}: {}", runId, code, LlmErrorClassifier.summary(ex));
			failAndRefund(runId, code);
		}
	}

	private void execute(UUID runId) {
		ValidationRun run = transactions.execute(status -> runs.findById(runId)
				.orElseThrow(() -> new LlmException("Validation run disappeared")));
		Idea idea = ideas.findById(run.getIdeaId())
				.orElseThrow(() -> new LlmException("Idea disappeared"));
		IdeaVersion version = versions.findById(run.getVersionId())
				.orElseThrow(() -> new LlmException("Version disappeared"));
		IdeaContent content = version.getContent();

		DeepResearchService.DeepBundle bundle = deepResearch.research(content);
		ScoringResponse response = llmGateway.generateJson(
				LlmPurpose.DEEP_VALIDATION,
				prompts.scoring(
						List.of(content),
						List.of(new ResearchBundle(bundle.evidence(), bundle.queries()))),
				ScoringResponse.class,
				validator);
		if (response.ideas() == null || response.ideas().isEmpty()) {
			throw new LlmException("Deep scoring returned no entries");
		}
		ScoringResponse.ScoredIdea scored = response.ideas().get(0);

		transactions.executeWithoutResult(status -> {
			ValidationRun managed = runs.findById(runId).orElseThrow();
			boolean evidenceAvailable = scored.evidenceAvailable() != null
					? scored.evidenceAvailable()
					: !bundle.evidence().isEmpty();
			validations.save(new IdeaValidation(
					managed.getIdeaId(),
					managed.getVersionId(),
					ValidationDepth.DEEP,
					scored.demandScore(),
					scored.profitabilityScore(),
					scored.competitionScore(),
					scored.noveltyScore(),
					evidenceAvailable,
					scored.rationales(),
					mergeCompetitors(scored, bundle),
					bundle.evidence(),
					bundle.queries()));
			managed.succeed();
		});
	}

	private List<com.app.ideaforge.ideas.dto.Competitor> mergeCompetitors(
			ScoringResponse.ScoredIdea scored,
			DeepResearchService.DeepBundle bundle) {
		java.util.List<com.app.ideaforge.ideas.dto.Competitor> merged = new java.util.ArrayList<>();
		if (scored.competitors() != null) {
			merged.addAll(scored.competitors());
		}
		merged.addAll(bundle.fetchedCompetitors());
		return merged.stream().limit(8).toList();
	}

	private void failAndRefund(UUID runId, String errorCode) {
		transactions.executeWithoutResult(status -> runs.findById(runId).ifPresent(run -> {
			if (run.getStatus() != ValidationRun.Status.RUNNING) {
				return;
			}
			run.fail(errorCode);
			if (run.getRefundLedgerId() == null && run.getSpendLedgerId() != null) {
				creditService.refund(run.getSpendLedgerId(), "Revalidation failed (" + errorCode + ")")
						.ifPresent(refund -> run.markRefunded(refund.getId()));
			}
		}));
	}

	private String errorCode(Exception ex) {
		return ex instanceof LlmException llm ? llm.getCode().runCode() : "INTERNAL_ERROR";
	}
}
