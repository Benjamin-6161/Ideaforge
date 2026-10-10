package com.app.ideaforge.generation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.fields.Field;
import com.app.ideaforge.fields.FieldRepository;
import com.app.ideaforge.generation.dto.GenerationParameters;
import com.app.ideaforge.generation.dto.IdeationResponse;
import com.app.ideaforge.generation.dto.ScoringResponse;
import com.app.ideaforge.ideas.Idea;
import com.app.ideaforge.ideas.IdeaRepository;
import com.app.ideaforge.ideas.IdeaValidation;
import com.app.ideaforge.ideas.IdeaValidationRepository;
import com.app.ideaforge.ideas.IdeaVersion;
import com.app.ideaforge.ideas.IdeaVersionRepository;
import com.app.ideaforge.ideas.ValidationDepth;
import com.app.ideaforge.ideas.VersionSource;
import com.app.ideaforge.ideas.dto.Competitor;
import com.app.ideaforge.ideas.dto.EvidenceItem;
import com.app.ideaforge.ideas.dto.IdeaContent;
import com.app.ideaforge.ideas.dto.ScoreRationales;
import com.app.ideaforge.llm.LlmErrorClassifier;
import com.app.ideaforge.llm.LlmException;
import com.app.ideaforge.llm.LlmGateway;
import com.app.ideaforge.llm.LlmPurpose;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;

/**
 * Async generation pipeline: ideation -> light research -> batched scoring ->
 * (optional) one replacement round. At most four logical LLM calls. Failures
 * mark the run FAILED and refund exactly once.
 */
@Service
public class GenerationPipeline {

	private static final Logger log = LoggerFactory.getLogger(GenerationPipeline.class);
	private static final int IDEA_COUNT = 5;
	private static final int WEAK_COMPETITION = 8;
	private static final int WEAK_DEMAND = 3;

	private final GenerationRunRepository runs;
	private final IdeaRepository ideas;
	private final IdeaVersionRepository versions;
	private final IdeaValidationRepository validations;
	private final FieldRepository fields;
	private final LlmGateway llmGateway;
	private final GenerationPrompts prompts;
	private final LightResearchService lightResearch;
	private final CreditService creditService;
	private final Validator validator;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactions;

	public GenerationPipeline(
			GenerationRunRepository runs,
			IdeaRepository ideas,
			IdeaVersionRepository versions,
			IdeaValidationRepository validations,
			FieldRepository fields,
			LlmGateway llmGateway,
			GenerationPrompts prompts,
			LightResearchService lightResearch,
			CreditService creditService,
			Validator validator,
			ObjectMapper objectMapper,
			PlatformTransactionManager transactionManager) {
		this.runs = runs;
		this.ideas = ideas;
		this.versions = versions;
		this.validations = validations;
		this.fields = fields;
		this.llmGateway = llmGateway;
		this.prompts = prompts;
		this.lightResearch = lightResearch;
		this.creditService = creditService;
		this.validator = validator;
		this.objectMapper = objectMapper;
		this.transactions = new TransactionTemplate(transactionManager);
	}

	/** Runs on the application executor; never throws to the caller. */
	@Async
	public void runAsync(UUID runId) {
		try {
			execute(runId);
		} catch (Exception ex) {
			String code = errorCode(ex);
			log.warn("Generation run {} failed with {}: {}", runId, code, LlmErrorClassifier.summary(ex));
			failAndRefund(runId, code);
		}
	}

	private void execute(UUID runId) {
		GenerationRun run = transactions.execute(status -> runs.findById(runId)
				.orElseThrow(() -> new LlmException("Generation run disappeared")));
		String promptVersion = prompts.ideationVersion();
		transactions.executeWithoutResult(status -> {
			GenerationRun managed = runs.findById(runId).orElseThrow();
			managed.start(promptVersion);
		});

		UUID userId = run.getUserId();
		UUID fieldId = run.getFieldId();
		String niche = run.getNiche();
		Field field = fields.findById(fieldId).orElseThrow(() -> new LlmException("Field no longer exists"));
		GenerationParameters parameters = objectMapper.convertValue(run.getParameters(), GenerationParameters.class);
		List<String> avoidTitles = ideas.findRecentTitles(userId, fieldId, PageRequest.of(0, 30));

		// 1. Ideation
		stage(runId, ProgressStage.IDEATING);
		IdeationResponse ideation = llmGateway.generateJson(
				LlmPurpose.IDEATION,
				prompts.ideation(field, niche, parameters, avoidTitles, IDEA_COUNT),
				IdeationResponse.class);
		List<IdeaContent> contents = new ArrayList<>(validatedIdeas(ideation, IDEA_COUNT));

		// 2. Light research per idea
		stage(runId, ProgressStage.RESEARCHING);
		List<ResearchBundle> bundles = new ArrayList<>(contents.stream().map(lightResearch::research).toList());

		// 3. Batched scoring
		stage(runId, ProgressStage.SCORING);
		List<ScoredIdea> scores = score(contents, bundles);

		// 4. One replacement round for weak ideas
		List<Integer> weak = weakIndexes(scores);
		if (!weak.isEmpty()) {
			replaceWeakIdeas(field, niche, parameters, avoidTitles, contents, bundles, scores, weak);
		}

		persistSuccess(runId, contents, bundles, scores);
	}

	private void replaceWeakIdeas(
			Field field,
			String niche,
			GenerationParameters parameters,
			List<String> avoidTitles,
			List<IdeaContent> contents,
			List<ResearchBundle> bundles,
			List<ScoredIdea> scores,
			List<Integer> weakIndexes) {
		Set<String> excluded = new HashSet<>(avoidTitles);
		contents.forEach(idea -> excluded.add(idea.title()));

		IdeationResponse replacementResponse = llmGateway.generateJson(
				LlmPurpose.IDEATION,
				prompts.ideation(field, niche, parameters, List.copyOf(excluded), weakIndexes.size()),
				IdeationResponse.class);
		List<IdeaContent> replacements = validatedIdeas(replacementResponse, weakIndexes.size());

		List<IdeaContent> replacedContents = new ArrayList<>();
		List<ResearchBundle> replacedBundles = new ArrayList<>();
		for (int i = 0; i < replacements.size() && i < weakIndexes.size(); i++) {
			int weakIndex = weakIndexes.get(i);
			IdeaContent replacement = replacements.get(i);
			contents.set(weakIndex, replacement);
			ResearchBundle bundle = lightResearch.research(replacement);
			bundles.set(weakIndex, bundle);
			replacedContents.add(replacement);
			replacedBundles.add(bundle);
		}
		if (replacedContents.isEmpty()) {
			return;
		}
		List<ScoredIdea> replacementScores = score(replacedContents, replacedBundles);
		for (int i = 0; i < replacementScores.size() && i < weakIndexes.size(); i++) {
			scores.set(weakIndexes.get(i), replacementScores.get(i));
		}
	}

	private List<ScoredIdea> score(List<IdeaContent> contents, List<ResearchBundle> bundles) {
		ScoringResponse response = llmGateway.generateJson(
				LlmPurpose.SCORING,
				prompts.scoring(contents, bundles),
				ScoringResponse.class);
		Map<Integer, ScoringResponse.ScoredIdea> byIndex = new java.util.HashMap<>();
		for (ScoringResponse.ScoredIdea scored : response.ideas()) {
			validate(scored, "scoring");
			byIndex.put(scored.index(), scored);
		}
		List<ScoredIdea> mapped = new ArrayList<>();
		for (int i = 0; i < contents.size(); i++) {
			ScoringResponse.ScoredIdea scored = byIndex.get(i);
			if (scored == null) {
				throw new LlmException("Scoring output is missing an entry for idea " + i);
			}
			boolean evidenceAvailable = scored.evidenceAvailable() != null
					? scored.evidenceAvailable()
					: !bundles.get(i).evidence().isEmpty();
			mapped.add(new ScoredIdea(
					scored.demandScore(),
					scored.profitabilityScore(),
					scored.competitionScore(),
					scored.noveltyScore(),
					evidenceAvailable,
					scored.rationales(),
					scored.competitors() == null ? List.of() : scored.competitors()));
		}
		return mapped;
	}

	private List<IdeaContent> validatedIdeas(IdeationResponse response, int requiredCount) {
		if (response.ideas() == null || response.ideas().size() < requiredCount) {
			throw new LlmException("Ideation output did not contain " + requiredCount + " ideas");
		}
		List<IdeaContent> contents = new ArrayList<>();
		for (IdeaContent idea : response.ideas().stream().limit(requiredCount).toList()) {
			validate(idea, "ideation");
			contents.add(idea);
		}
		return contents;
	}

	private List<Integer> weakIndexes(List<ScoredIdea> scores) {
		List<Integer> weak = new ArrayList<>();
		for (int i = 0; i < scores.size(); i++) {
			ScoredIdea score = scores.get(i);
			if (score.competition() >= WEAK_COMPETITION || score.demand() <= WEAK_DEMAND) {
				weak.add(i);
			}
		}
		return weak;
	}

	private void persistSuccess(
			UUID runId,
			List<IdeaContent> contents,
			List<ResearchBundle> bundles,
			List<ScoredIdea> scores) {
		transactions.executeWithoutResult(status -> {
			GenerationRun managed = runs.findById(runId).orElseThrow();
			for (int i = 0; i < contents.size(); i++) {
				IdeaContent content = contents.get(i);
				ScoredIdea score = scores.get(i);
				ResearchBundle bundle = bundles.get(i);

				Idea idea = new Idea(
						managed.getUserId(), managed.getId(), managed.getFieldId(), managed.getNiche(),
						content.title(), content.tagline());
				ideas.save(idea);
				IdeaVersion version = new IdeaVersion(
						idea.getId(), 1, VersionSource.GENERATION, content, "Initial generation");
				versions.save(version);
				idea.applyContent(content.title(), content.tagline(), version.getId());
				validations.save(new IdeaValidation(
						idea.getId(),
						version.getId(),
						ValidationDepth.LIGHT,
						score.demand(),
						score.profitability(),
						score.competition(),
						score.novelty(),
						score.evidenceAvailable(),
						score.rationales(),
						score.competitors(),
						bundle.evidence(),
						bundle.queries()));
			}
			managed.succeed();
		});
	}

	private void stage(UUID runId, ProgressStage stage) {
		transactions.executeWithoutResult(status ->
				runs.findById(runId).ifPresent(run -> run.stage(stage)));
	}

	private void failAndRefund(UUID runId, String errorCode) {
		transactions.executeWithoutResult(status -> runs.findById(runId).ifPresent(run -> {
			if (run.getStatus() == RunStatus.SUCCEEDED) {
				return;
			}
			// Mark failed first; refund updates the wallet via a modifying query.
			run.fail(errorCode);
			if (run.getRefundLedgerId() == null && run.getSpendLedgerId() != null) {
				creditService.refund(run.getSpendLedgerId(), "Generation failed (" + errorCode + ")")
						.ifPresent(refund -> run.markRefunded(refund.getId()));
			}
		}));
	}

	private <T> void validate(T value, String what) {
		Set<ConstraintViolation<T>> violations = validator.validate(value);
		if (!violations.isEmpty()) {
			ConstraintViolation<T> first = violations.iterator().next();
			throw new LlmException(what + " output failed validation: " + first.getPropertyPath() + " " + first.getMessage());
		}
	}

	private String errorCode(Exception ex) {
		return ex instanceof LlmException llm ? llm.getCode().runCode() : "INTERNAL_ERROR";
	}

	record ScoredIdea(
			int demand,
			int profitability,
			int competition,
			int novelty,
			boolean evidenceAvailable,
			ScoreRationales rationales,
			List<Competitor> competitors) {
	}
}
