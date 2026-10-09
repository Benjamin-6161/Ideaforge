package com.app.ideaforge.ideas;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.app.ideaforge.IntegrationTestBase;
import com.app.ideaforge.credits.CreditLedgerEntry;
import com.app.ideaforge.credits.CreditLedgerRepository;
import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.credits.LedgerEntryType;
import com.app.ideaforge.fields.FieldRepository;
import com.app.ideaforge.generation.TestIdeas;
import com.app.ideaforge.generation.dto.ScoringResponse;
import com.app.ideaforge.ideas.dto.ScoreRationales;
import com.app.ideaforge.llm.FakeLlmGateway;
import com.app.ideaforge.llm.LlmException;
import com.app.ideaforge.research.CommunitySignalProvider;
import com.app.ideaforge.research.SearchProvider;
import com.app.ideaforge.research.SearchResult;
import com.app.ideaforge.testconfig.FakeLlmConfiguration;
import com.app.ideaforge.user.EmailNormalizer;
import com.app.ideaforge.user.UserRepository;

import jakarta.servlet.http.Cookie;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(FakeLlmConfiguration.class)
class RevalidationApiIntegrationTest extends IntegrationTestBase {

	@MockitoBean
	SearchProvider searchProvider;

	@MockitoBean
	CommunitySignalProvider communitySignalProvider;

	@Autowired
	FakeLlmGateway fakeLlm;

	@Autowired
	CreditService creditService;

	@Autowired
	CreditLedgerRepository ledgerRepository;

	@Autowired
	IdeaRepository ideas;

	@Autowired
	IdeaVersionRepository versions;

	@Autowired
	IdeaValidationRepository validations;

	@Autowired
	ValidationRunRepository runs;

	@Autowired
	ValidationRecoveryJob recoveryJob;

	@Autowired
	FieldRepository fields;

	@Autowired
	UserRepository users;

	@Autowired
	EmailNormalizer emailNormalizer;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private String lastEmail;

	@BeforeEach
	void setUp() {
		fakeLlm.reset();
		when(searchProvider.search(anyString(), anyInt())).thenAnswer(invocation -> List.of(
				new SearchResult("Competitor A", "https://competitor-a.example", "Broad tool")));
		when(communitySignalProvider.search(anyString(), anyInt())).thenAnswer(invocation -> List.of());
	}

	@Test
	void revalidationProducesADeepValidation() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId);
		fakeLlm.json(ScoringResponse.class, TestIdeas.strongScores(1));

		MvcResult created = mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/revalidate"))
				.cookie(cookie)
				.header("Idempotency-Key", "revalidate-1"))
				.andExpect(status().isAccepted())
				.andReturn();
		assertThat(created.getResponse().getContentAsString()).contains("RUNNING");

		Map<String, Object> detail = awaitRevalidation(cookie, idea.getId());
		@SuppressWarnings("unchecked")
		Map<String, Object> revalidation = (Map<String, Object>) detail.get("revalidation");
		assertThat(revalidation.get("status")).isEqualTo("SUCCEEDED");

		IdeaValidation deep = validations.findFirstByIdeaIdOrderByCreatedAtDesc(idea.getId()).orElseThrow();
		assertThat(deep.getDepth()).isEqualTo(ValidationDepth.DEEP);
		assertThat(deep.getVersionId()).isEqualTo(idea.getCurrentVersionId());
		assertThat(deep.getEvidence()).isNotEmpty();
		assertThat(creditService.balanceUnits(userId)).isEqualTo(45); // 50 - 5
	}

	@Test
	void failedRevalidationRefunds() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId);
		fakeLlm.failWith(new LlmException("boom"));

		mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/revalidate"))
				.cookie(cookie)
				.header("Idempotency-Key", "revalidate-2"))
				.andExpect(status().isAccepted());

		Map<String, Object> detail = awaitRevalidation(cookie, idea.getId());
		@SuppressWarnings("unchecked")
		Map<String, Object> revalidation = (Map<String, Object>) detail.get("revalidation");
		assertThat(revalidation.get("status")).isEqualTo("FAILED");
		assertThat(revalidation.get("errorCode")).isEqualTo("LLM_FAILED");
		assertThat(creditService.balanceUnits(userId)).isEqualTo(50);
		List<CreditLedgerEntry> ledger = ledgerRepository
				.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10)).getContent();
		assertThat(ledger).anyMatch(entry -> entry.getEntryType() == LedgerEntryType.REFUND);
	}

	@Test
	void activeRevalidationBlocksANewOne() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId);
		runs.save(new ValidationRun(idea.getId(), userId, idea.getCurrentVersionId(), null));

		MvcResult result = mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/revalidate"))
				.cookie(cookie)
				.header("Idempotency-Key", "revalidate-3"))
				.andExpect(status().isConflict())
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).contains("CONFLICTING_JOB");
		assertThat(creditService.balanceUnits(userId)).isEqualTo(50);
	}

	@Test
	void finalizedIdeasCannotBeRevalidated() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId);
		idea.setStatus(IdeaStatus.FINALIZED);
		ideas.save(idea);

		MvcResult result = mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/revalidate"))
				.cookie(cookie)
				.header("Idempotency-Key", "revalidate-4"))
				.andExpect(status().isConflict())
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).contains("IDEA_LOCKED");
	}

	@Test
	void startupRecoveryRefundsStaleRevalidations() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId);
		CreditLedgerEntry spend = creditService.spend(userId, "REVALIDATE", "validation_run", null, "stale-reval");
		ValidationRun stale = runs.save(new ValidationRun(idea.getId(), userId, idea.getCurrentVersionId(), spend.getId()));

		recoveryJob.run(null);

		ValidationRun recovered = runs.findById(stale.getId()).orElseThrow();
		assertThat(recovered.getStatus()).isEqualTo(ValidationRun.Status.FAILED);
		assertThat(recovered.getErrorCode()).isEqualTo("APP_RESTART");
		assertThat(creditService.balanceUnits(userId)).isEqualTo(50);
	}

	@Test
	void revalidationIsScopedToTheOwner() throws Exception {
		Cookie owner = login(uniqueEmail());
		Idea idea = seedIdea(currentUserId());

		Cookie stranger = login(uniqueEmail());
		mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/revalidate"))
				.cookie(stranger)
				.header("Idempotency-Key", "revalidate-5"))
				.andExpect(status().isNotFound());
	}

	// ---- helpers ----

	private String uniqueEmail() {
		lastEmail = "reval-" + UUID.randomUUID() + "@example.com";
		return lastEmail;
	}

	private UUID currentUserId() {
		return users.findByEmailNormalized(emailNormalizer.normalize(lastEmail)).orElseThrow().getId();
	}

	private Idea seedIdea(UUID userId) {
		UUID fieldId = fields.findByActiveTrueAndParentIdIsNullOrderBySortOrderAsc().get(0).getId();
		Idea idea = new Idea(userId, null, fieldId, null, "Dental reminders", "tagline");
		ideas.save(idea);
		IdeaVersion version = versions.save(new IdeaVersion(
				idea.getId(), 1, VersionSource.GENERATION, TestIdeas.idea("Dental reminders"), "Initial"));
		idea.applyContent("Dental reminders", "tagline", version.getId());
		ideas.save(idea);
		validations.save(new IdeaValidation(
				idea.getId(), version.getId(), ValidationDepth.LIGHT,
				7, 7, 4, 6, true, new ScoreRationales("d", "p", "c", "n"),
				List.of(), List.of(), List.of()));
		return idea;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> awaitRevalidation(Cookie cookie, UUID ideaId) throws Exception {
		for (int i = 0; i < 100; i++) {
			MvcResult result = mockMvc.perform(get("/api/ideas/" + ideaId).cookie(cookie))
					.andExpect(status().isOk())
					.andReturn();
			Map<String, Object> detail = objectMapper.readValue(
					result.getResponse().getContentAsString(), new TypeReference<>() {
					});
			Map<String, Object> revalidation = (Map<String, Object>) detail.get("revalidation");
			if (revalidation != null && !"RUNNING".equals(revalidation.get("status"))) {
				return detail;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Revalidation did not finish in time");
	}
}
