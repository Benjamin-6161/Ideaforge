package com.app.ideaforge.finalization;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.finalization.dto.FinalizePlan;
import com.app.ideaforge.generation.GenerationPrompts;
import com.app.ideaforge.generation.ResearchBundle;
import com.app.ideaforge.generation.dto.ScoringResponse;
import com.app.ideaforge.ideas.Idea;
import com.app.ideaforge.ideas.IdeaRepository;
import com.app.ideaforge.ideas.IdeaValidation;
import com.app.ideaforge.ideas.IdeaValidationRepository;
import com.app.ideaforge.ideas.IdeaVersion;
import com.app.ideaforge.ideas.IdeaVersionRepository;
import com.app.ideaforge.ideas.ValidationDepth;
import com.app.ideaforge.ideas.dto.IdeaContent;
import com.app.ideaforge.llm.LlmErrorClassifier;
import com.app.ideaforge.llm.LlmException;
import com.app.ideaforge.llm.LlmGateway;
import com.app.ideaforge.llm.LlmPurpose;
import com.app.ideaforge.research.DeepResearchService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Finalization pipeline: deep validation, then the plan, build prompt, and
 * AGENTS.md in separate calls. Failures refund exactly once.
 */
@Service
public class FinalizePipeline {

	private static final Logger log = LoggerFactory.getLogger(FinalizePipeline.class);
	private static final int MAX_DOCUMENT_CHARS = 120_000;

	private final FinalizationRepository finalizations;
	private final IdeaRepository ideas;
	private final IdeaVersionRepository versions;
	private final IdeaValidationRepository validations;
	private final DeepResearchService deepResearch;
	private final GenerationPrompts generationPrompts;
	private final FinalizePrompts finalizePrompts;
	private final LlmGateway llmGateway;
	private final CreditService creditService;
	private final Validator validator;
	private final TransactionTemplate transactions;

	public FinalizePipeline(
			FinalizationRepository finalizations,
			IdeaRepository ideas,
			IdeaVersionRepository versions,
			IdeaValidationRepository validations,
			DeepResearchService deepResearch,
			GenerationPrompts generationPrompts,
			FinalizePrompts finalizePrompts,
			LlmGateway llmGateway,
			CreditService creditService,
			Validator validator,
			PlatformTransactionManager transactionManager) {
		this.finalizations = finalizations;
		this.ideas = ideas;
		this.versions = versions;
		this.validations = validations;
		this.deepResearch = deepResearch;
		this.generationPrompts = generationPrompts;
		this.finalizePrompts = finalizePrompts;
		this.llmGateway = llmGateway;
		this.creditService = creditService;
		this.validator = validator;
		this.transactions = new TransactionTemplate(transactionManager);
	}

	@Async
	public void runAsync(UUID finalizationId) {
		try {
			execute(finalizationId);
		} catch (Exception ex) {
			String code = errorCode(ex);
			log.warn("Finalization {} failed with {}: {}", finalizationId, code, LlmErrorClassifier.summary(ex));
			failAndRefund(finalizationId, code);
		}
	}

	private void execute(UUID finalizationId) {
		Finalization finalization = transactions.execute(status -> finalizations.findById(finalizationId)
				.orElseThrow(() -> new LlmException("Finalization disappeared")));
		Idea idea = ideas.findById(finalization.getIdeaId())
				.orElseThrow(() -> new LlmException("Idea disappeared"));
		IdeaVersion version = versions.findById(finalization.getVersionId())
				.orElseThrow(() -> new LlmException("Version disappeared"));
		IdeaContent content = version.getContent();

		// Deep validation at finalize time.
		DeepResearchService.DeepBundle bundle = deepResearch.research(content);
		ScoringResponse scoring = llmGateway.generateJson(
				LlmPurpose.DEEP_VALIDATION,
				generationPrompts.scoring(
						List.of(content),
						List.of(new ResearchBundle(bundle.evidence(), bundle.queries()))),
				ScoringResponse.class);
		if (scoring.ideas() == null || scoring.ideas().isEmpty()) {
			throw new LlmException("Deep scoring returned no entries");
		}
		ScoringResponse.ScoredIdea scored = scoring.ideas().get(0);
		validate(scored, "deep scoring");

		FinalizePlan plan = llmGateway.generateJson(
				LlmPurpose.FINALIZE,
				finalizePrompts.plan(content, validationSummary(scored, bundle)),
				FinalizePlan.class);
		validate(plan, "finalize plan");

		String buildPrompt = document("build prompt",
				llmGateway.generateText(LlmPurpose.FINALIZE, finalizePrompts.buildPrompt(content, plan)).text());
		String agentsMd = document("AGENTS.md",
				llmGateway.generateText(LlmPurpose.FINALIZE, finalizePrompts.agentsMd(content, plan)).text());

		transactions.executeWithoutResult(status -> {
			Finalization managed = finalizations.findById(finalizationId).orElseThrow();
			Idea managedIdea = ideas.findById(managed.getIdeaId()).orElseThrow();
			boolean evidenceAvailable = scored.evidenceAvailable() != null
					? scored.evidenceAvailable()
					: !bundle.evidence().isEmpty();
			validations.save(new IdeaValidation(
					managedIdea.getId(),
					managed.getVersionId(),
					ValidationDepth.DEEP,
					scored.demandScore(),
					scored.profitabilityScore(),
					scored.competitionScore(),
					scored.noveltyScore(),
					evidenceAvailable,
					scored.rationales(),
					scored.competitors() == null ? List.of() : scored.competitors(),
					bundle.evidence(),
					bundle.queries()));
			managed.succeed(plan, buildPrompt, agentsMd);
			managedIdea.markFinalized();
		});
	}

	private String validationSummary(ScoringResponse.ScoredIdea scored, DeepResearchService.DeepBundle bundle) {
		return "demand " + scored.demandScore() + "/10; profitability " + scored.profitabilityScore()
				+ "/10; competition " + scored.competitionScore() + "/10 (10 = saturated); novelty "
				+ scored.noveltyScore() + "/10; evidence sources: " + bundle.evidence().size()
				+ ". Demand rationale: " + scored.rationales().demand();
	}

	private String document(String name, String text) {
		if (text == null || text.isBlank()) {
			throw new LlmException("The " + name + " document was empty");
		}
		if (text.length() > MAX_DOCUMENT_CHARS) {
			throw new LlmException("The " + name + " document exceeds the maximum allowed length");
		}
		return text;
	}

	private <T> void validate(T value, String what) {
		Set<ConstraintViolation<T>> violations = validator.validate(value);
		if (!violations.isEmpty()) {
			ConstraintViolation<T> first = violations.iterator().next();
			throw new LlmException(what + " failed validation: " + first.getPropertyPath() + " " + first.getMessage());
		}
	}

	private void failAndRefund(UUID finalizationId, String errorCode) {
		transactions.executeWithoutResult(status -> finalizations.findById(finalizationId).ifPresent(finalization -> {
			if (finalization.getStatus() != Finalization.Status.RUNNING) {
				return;
			}
			finalization.fail();
			if (finalization.getRefundLedgerId() == null && finalization.getSpendLedgerId() != null) {
				creditService.refund(finalization.getSpendLedgerId(), "Finalization failed (" + errorCode + ")")
						.ifPresent(refund -> finalization.markRefunded(refund.getId()));
			}
		}));
	}

	private String errorCode(Exception ex) {
		return ex instanceof LlmException llm ? llm.getCode().runCode() : "INTERNAL_ERROR";
	}
}
