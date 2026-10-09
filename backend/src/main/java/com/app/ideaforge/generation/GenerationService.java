package com.app.ideaforge.generation;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.credits.CreditLedgerEntry;
import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.fields.Field;
import com.app.ideaforge.fields.FieldService;
import com.app.ideaforge.generation.dto.CreateGenerationRequest;
import com.app.ideaforge.generation.dto.GenerationParameters;
import com.app.ideaforge.generation.dto.GenerationRunResponse;
import com.app.ideaforge.ideas.Idea;
import com.app.ideaforge.ideas.IdeaRepository;
import com.app.ideaforge.ideas.IdeaValidation;
import com.app.ideaforge.ideas.IdeaValidationRepository;
import com.app.ideaforge.ideas.IdeaVersionRepository;
import com.app.ideaforge.security.ClientIpHasher;
import com.app.ideaforge.security.InputGuard;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Request side of the generation flow: validate, rate-limit, charge with
 * idempotency, create the run, then hand off to the async pipeline.
 */
@Service
public class GenerationService {

	private final GenerationRunRepository runs;
	private final FieldService fieldService;
	private final CreditService creditService;
	private final GenerationPipeline pipeline;
	private final InputGuard inputGuard;
	private final ClientIpHasher ipHasher;
	private final ObjectMapper objectMapper;
	private final AppProperties properties;
	private final java.time.Clock clock;
	private final IdeaRepository ideas;
	private final IdeaVersionRepository versions;
	private final IdeaValidationRepository validations;

	public GenerationService(
			GenerationRunRepository runs,
			FieldService fieldService,
			CreditService creditService,
			GenerationPipeline pipeline,
			InputGuard inputGuard,
			ClientIpHasher ipHasher,
			ObjectMapper objectMapper,
			AppProperties properties,
			java.time.Clock clock,
			IdeaRepository ideas,
			IdeaVersionRepository versions,
			IdeaValidationRepository validations) {
		this.runs = runs;
		this.fieldService = fieldService;
		this.creditService = creditService;
		this.pipeline = pipeline;
		this.inputGuard = inputGuard;
		this.ipHasher = ipHasher;
		this.objectMapper = objectMapper;
		this.properties = properties;
		this.clock = clock;
		this.ideas = ideas;
		this.versions = versions;
		this.validations = validations;
	}

	@Transactional
	public GenerationRunResponse create(
			UUID userId,
			String idempotencyKey,
			CreateGenerationRequest request,
			HttpServletRequest httpRequest) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw ApiException.badRequest(ErrorCodes.IDEMPOTENCY_KEY_REQUIRED,
					"An Idempotency-Key header is required for this endpoint.");
		}
		if (idempotencyKey.length() > 100) {
			throw ApiException.badRequest(ErrorCodes.VALIDATION_ERROR, "Idempotency-Key is too long.");
		}
		// One creation at a time per user; also protects the replay check.
		runs.lockUser(userId);
		String ipHash = ipHasher.hash(httpRequest);
		Field field = fieldService.requireActive(request.fieldId());
		String niche = inputGuard.guardSingleLine(request.niche(), 100, userId, ipHash, "niche");
		GenerationParameters raw = request.parametersOrDefault();
		GenerationParameters parameters = new GenerationParameters(
				raw.targetCustomer(),
				raw.customerSize(),
				raw.builderProfile(),
				raw.timeToMvp(),
				raw.startingBudget(),
				raw.aiDepth(),
				raw.aiCapabilities(),
				raw.monetization(),
				raw.region(),
				raw.noveltyPreference(),
				inputGuard.guardSingleLine(raw.problemFocus(), 200, userId, ipHash, "problemFocus"),
				inputGuard.guardSingleLine(raw.exclude(), 200, userId, ipHash, "exclude"));

		Map<String, Object> parameterMap = objectMapper.convertValue(parameters, new TypeReference<>() {
		});

		Optional<CreditLedgerEntry> previousSpend = creditService.findByIdempotencyKey(userId, idempotencyKey);
		if (previousSpend.isPresent()) {
			Optional<GenerationRun> previousRun = runs.findBySpendLedgerId(previousSpend.get().getId());
			if (previousRun.isPresent()) {
				return respond(previousRun.get());
			}
			// Orphaned spend (crash between charge and run creation): reuse it.
			GenerationRun run = new GenerationRun(
					userId, field.getId(), niche, parameterMap, previousSpend.get().getId());
			runs.save(run);
			scheduleAfterCommit(run.getId());
			return respond(run);
		}

		assertDailyLimit(userId);
		assertNoActiveRun(userId);

		CreditLedgerEntry spend = creditService.spend(
				userId, "GENERATE_IDEAS", "generation_run", null, idempotencyKey);
		Optional<GenerationRun> raced = runs.findBySpendLedgerId(spend.getId());
		if (raced.isPresent()) {
			return respond(raced.get());
		}

		GenerationRun run = new GenerationRun(userId, field.getId(), niche, parameterMap, spend.getId());
		runs.save(run);
		scheduleAfterCommit(run.getId());
		return respond(run);
	}

	@Transactional(readOnly = true)
	public GenerationRunResponse get(UUID userId, UUID runId) {
		GenerationRun run = runs.findByIdAndUserId(runId, userId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Generation not found."));
		return respond(run);
	}

	private GenerationRunResponse respond(GenerationRun run) {
		List<GenerationRunResponse.GeneratedIdeaSummary> ideaSummaries = List.of();
		if (run.getStatus() == RunStatus.SUCCEEDED) {
			List<Idea> runIdeas = ideas.findByGenerationRunIdAndDeletedAtIsNullOrderByCreatedAtAsc(run.getId());
			ideaSummaries = runIdeas.stream().map(this::summarize).toList();
		}
		return new GenerationRunResponse(
				run.getId(),
				run.getStatus().name(),
				run.getProgressStage() == null ? null : run.getProgressStage().name(),
				run.getErrorCode(),
				run.getFieldId(),
				run.getNiche(),
				run.getCreatedAt(),
				run.getCompletedAt(),
				ideaSummaries);
	}

	private GenerationRunResponse.GeneratedIdeaSummary summarize(Idea idea) {
		IdeaValidation validation = validations.findFirstByIdeaIdOrderByCreatedAtDesc(idea.getId()).orElse(null);
		GenerationRunResponse.Scores scores = validation == null
				? null
				: new GenerationRunResponse.Scores(
						validation.getDemandScore(),
						validation.getProfitabilityScore(),
						validation.getCompetitionScore(),
						validation.getNoveltyScore(),
						validation.isEvidenceAvailable(),
						validation.getDepth().name(),
						validation.getVersionId());
		int versionNumber = idea.getCurrentVersionId() == null
				? 1
				: versions.findById(idea.getCurrentVersionId()).map(v -> v.getVersionNumber()).orElse(1);
		return new GenerationRunResponse.GeneratedIdeaSummary(
				idea.getId(),
				idea.getTitle(),
				idea.getTagline(),
				idea.getFieldId(),
				idea.getStatus().name(),
				idea.getCurrentVersionId(),
				versionNumber,
				scores);
	}

	private void assertDailyLimit(UUID userId) {
		java.time.Instant startOfDay = clock.instant().truncatedTo(ChronoUnit.DAYS);
		long today = runs.countByUserIdAndCreatedAtAfter(userId, startOfDay);
		if (today >= properties.limits().generationsPerDay()) {
			throw new ApiException(
					HttpStatus.TOO_MANY_REQUESTS,
					ErrorCodes.DAILY_LIMIT_REACHED,
					"Daily generation limit reached. Try again tomorrow.");
		}
	}

	private void assertNoActiveRun(UUID userId) {
		if (runs.existsByUserIdAndStatusIn(userId, List.of(RunStatus.PENDING, RunStatus.RUNNING))) {
			throw ApiException.conflict(ErrorCodes.CONFLICTING_JOB,
					"Another generation is already running.");
		}
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
}
