package com.app.ideaforge.finalization;

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
import com.app.ideaforge.credits.CreditLedgerRepository;
import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.credits.LedgerEntryType;
import com.app.ideaforge.fields.FieldRepository;
import com.app.ideaforge.finalization.dto.FinalizePlan;
import com.app.ideaforge.generation.TestIdeas;
import com.app.ideaforge.generation.dto.ScoringResponse;
import com.app.ideaforge.ideas.Idea;
import com.app.ideaforge.ideas.IdeaRepository;
import com.app.ideaforge.ideas.IdeaStatus;
import com.app.ideaforge.ideas.IdeaValidation;
import com.app.ideaforge.ideas.IdeaValidationRepository;
import com.app.ideaforge.ideas.IdeaVersion;
import com.app.ideaforge.ideas.IdeaVersionRepository;
import com.app.ideaforge.ideas.ValidationDepth;
import com.app.ideaforge.ideas.VersionSource;
import com.app.ideaforge.ideas.dto.ScoreRationales;
import com.app.ideaforge.llm.FakeLlmGateway;
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
class FinalizeApiIntegrationTest extends IntegrationTestBase {

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
	FinalizationRepository finalizations;

	@Autowired
	FinalizationRecoveryJob recoveryJob;

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
		when(communitySignalProvider.search(anyString(), anyInt())).thenReturn(List.of());
	}

	@Test
	void finalizeProducesTheFullPackageAndLocksTheIdea() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId);
		stubFinalizeResponses();

		MvcResult created = mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/finalize"))
				.cookie(cookie)
				.header("Idempotency-Key", "finalize-1"))
				.andExpect(status().isAccepted())
				.andReturn();
		assertThat(created.getResponse().getContentAsString()).contains("RUNNING");

		Map<String, Object> response = awaitFinalization(cookie, idea.getId());
		assertThat(response.get("status")).isEqualTo("SUCCEEDED");
		assertThat(response.get("buildPromptMd").toString()).contains("# Dental reminders");
		assertThat(response.get("agentsMd").toString()).contains("# AGENTS.md");
		@SuppressWarnings("unchecked")
		Map<String, Object> output = (Map<String, Object>) response.get("output");
		assertThat(output).containsKeys("techStack", "mvpRoadmap", "pricingSuggestion", "launchChecklist", "risks");

		Idea locked = ideas.findById(idea.getId()).orElseThrow();
		assertThat(locked.getStatus()).isEqualTo(IdeaStatus.FINALIZED);
		assertThat(locked.getFinalizedAt()).isNotNull();
		assertThat(creditService.balanceUnits(userId)).isEqualTo(40);
		assertThat(validations.findFirstByIdeaIdOrderByCreatedAtDesc(idea.getId()).orElseThrow().getDepth())
				.isEqualTo(ValidationDepth.DEEP);
	}

	@Test
	void idempotentRetryReturnsTheSameFinalization() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId);
		stubFinalizeResponses();

		MvcResult first = mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/finalize"))
				.cookie(cookie)
				.header("Idempotency-Key", "finalize-2"))
				.andExpect(status().isAccepted())
				.andReturn();
		awaitFinalization(cookie, idea.getId());
		MvcResult second = mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/finalize"))
				.cookie(cookie)
				.header("Idempotency-Key", "finalize-2"))
				.andExpect(status().isAccepted())
				.andReturn();

		assertThat(parse(second).get("id")).isEqualTo(parse(first).get("id"));
		assertThat(creditService.balanceUnits(userId)).isEqualTo(40);
	}

	@Test
	void failureRefundsAndUnlocksTheIdea() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId);
		fakeLlm.failWith(new com.app.ideaforge.llm.LlmException("boom"));

		mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/finalize"))
				.cookie(cookie)
				.header("Idempotency-Key", "finalize-3"))
				.andExpect(status().isAccepted());

		Map<String, Object> response = awaitFinalization(cookie, idea.getId());
		assertThat(response.get("status")).isEqualTo("FAILED");
		assertThat(creditService.balanceUnits(userId)).isEqualTo(50);
		assertThat(ideas.findById(idea.getId()).orElseThrow().getStatus()).isNotEqualTo(IdeaStatus.FINALIZED);
		assertThat(ledgerRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10)).getContent())
				.anyMatch(entry -> entry.getEntryType() == LedgerEntryType.REFUND);
	}

	@Test
	void alreadyFinalizedIdeasAreRejected() throws Exception {
		Cookie cookie = login(uniqueEmail());
		Idea idea = seedIdea(currentUserId());
		idea.setStatus(IdeaStatus.FINALIZED);
		ideas.save(idea);

		MvcResult result = mockMvc.perform(withIp(post("/api/ideas/" + idea.getId() + "/finalize"))
				.cookie(cookie)
				.header("Idempotency-Key", "finalize-4"))
				.andExpect(status().isConflict())
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).contains("IDEA_LOCKED");
	}

	@Test
	void nonFinalizedIdeasHaveNoFinalization() throws Exception {
		Cookie cookie = login(uniqueEmail());
		Idea idea = seedIdea(currentUserId());

		// Existing completed finalization for a draft idea (e.g. failed earlier) is not required.
		mockMvc.perform(get("/api/ideas/" + idea.getId() + "/finalization").cookie(cookie))
				.andExpect(status().isNotFound());
	}

	@Test
	void finalizationIsScopedToTheOwner() throws Exception {
		Cookie owner = login(uniqueEmail());
		Idea idea = seedIdea(currentUserId());

		Cookie stranger = login(uniqueEmail());
		mockMvc.perform(get("/api/ideas/" + idea.getId() + "/finalization").cookie(stranger))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/ideas/" + idea.getId() + "/finalize")
				.cookie(stranger)
				.header("Idempotency-Key", "finalize-5")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	void startupRecoveryRefundsStaleFinalizations() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId);
		var spend = creditService.spend(userId, "FINALIZE", "finalization", idea.getId(), "stale-finalize");
		Finalization stale = finalizations.save(
				new Finalization(idea.getId(), idea.getCurrentVersionId(), spend.getId(), "test"));

		recoveryJob.run(null);

		Finalization recovered = finalizations.findById(stale.getId()).orElseThrow();
		assertThat(recovered.getStatus()).isEqualTo(Finalization.Status.FAILED);
		assertThat(creditService.balanceUnits(userId)).isEqualTo(50);
	}

	// ---- helpers ----

	private void stubFinalizeResponses() {
		fakeLlm.json(ScoringResponse.class, TestIdeas.strongScores(1));
		fakeLlm.json(FinalizePlan.class, plan());
		fakeLlm.textSequence("# Dental reminders — Build Prompt\n\n## Product\n...", "# AGENTS.md\n\n## Project\n...");
	}

	private FinalizePlan plan() {
		return new FinalizePlan(
				new FinalizePlan.TechStack(
						"React + Vite", "Spring Boot", "PostgreSQL", "Email OTP sessions", "Single VPS",
						"Claude via LangChain4j", List.of("Sentry free tier"), "$10-25/month", "Free tiers until traction"),
				List.of(
						new FinalizePlan.RoadmapPhase("Foundation", List.of("Auth and schema"), "1-2"),
						new FinalizePlan.RoadmapPhase("Core loop", List.of("Reminders"), "2-3"),
						new FinalizePlan.RoadmapPhase("Launch", List.of("Billing"), "2")),
				"$99/month per location",
				List.of("Landing page", "Waitlist", "Pricing page", "Legal pages", "Support email", "Analytics"),
				List.of("Interview 10 practice managers", "Run a fake-door test", "Check competitor pricing"),
				new FinalizePlan.Branding(List.of("NoShow Shield", "DentalNudge", "ChairFill"), "Use .com or country TLDs"),
				List.of("Data quality", "Integration coverage", "Reminder fatigue"));
	}

	private String uniqueEmail() {
		lastEmail = "final-" + UUID.randomUUID() + "@example.com";
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
	private Map<String, Object> awaitFinalization(Cookie cookie, UUID ideaId) throws Exception {
		for (int i = 0; i < 150; i++) {
			MvcResult result = mockMvc.perform(get("/api/ideas/" + ideaId + "/finalization").cookie(cookie))
					.andExpect(status().isOk())
					.andReturn();
			Map<String, Object> response = parse(result);
			if (!"RUNNING".equals(response.get("status"))) {
				return response;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Finalization did not finish in time");
	}

	private Map<String, Object> parse(MvcResult result) throws Exception {
		return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
		});
	}
}
