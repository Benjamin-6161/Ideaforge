package com.app.ideaforge.ideas;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.app.ideaforge.IntegrationTestBase;
import com.app.ideaforge.fields.FieldRepository;
import com.app.ideaforge.generation.TestIdeas;
import com.app.ideaforge.ideas.dto.ScoreRationales;
import com.app.ideaforge.user.EmailNormalizer;
import com.app.ideaforge.user.User;
import com.app.ideaforge.user.UserRepository;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdeasApiIntegrationTest extends IntegrationTestBase {

	@Autowired
	IdeaRepository ideas;

	@Autowired
	IdeaVersionRepository versions;

	@Autowired
	IdeaValidationRepository validations;

	@Autowired
	FieldRepository fields;

	@Autowired
	UserRepository users;

	@Autowired
	EmailNormalizer emailNormalizer;

	@Test
	void listRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/ideas")).andExpect(status().isUnauthorized());
	}

	@Test
	void listFiltersAndSortsOwnIdeas() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		seedIdea(userId, fieldId(), "Low demand idea", 2, false, IdeaStatus.GENERATED, false);
		seedIdea(userId, fieldId(), "High demand idea", 9, true, IdeaStatus.GENERATED, false);
		seedIdea(userId, fieldId(), "Archived idea", 7, false, IdeaStatus.GENERATED, true);
		seedIdea(userId, fieldId(), "In progress idea", 5, false, IdeaStatus.IN_PROGRESS, false);

		mockMvc.perform(get("/api/ideas").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(3));

		mockMvc.perform(get("/api/ideas").param("sort", "demand").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].title").value("High demand idea"))
				.andExpect(jsonPath("$.items[0].scores.demand").value(9));

		mockMvc.perform(get("/api/ideas").param("status", "IN_PROGRESS").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].title").value("In progress idea"));

		mockMvc.perform(get("/api/ideas").param("favorite", "true").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].title").value("High demand idea"));

		mockMvc.perform(get("/api/ideas").param("q", "demand").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(2));

		mockMvc.perform(get("/api/ideas").param("archived", "true").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].title").value("Archived idea"));
	}

	@Test
	void listIsScopedToTheAuthenticatedUser() throws Exception {
		login(uniqueEmail());
		UUID ownerId = currentUserId();
		seedIdea(ownerId, fieldId(), "Private idea", 8, false, IdeaStatus.GENERATED, false);
		UUID privateIdeaId = ideas.findAll().stream()
				.filter(idea -> idea.getUserId().equals(ownerId))
				.findFirst()
				.orElseThrow()
				.getId();

		Cookie stranger = login(uniqueEmail());
		mockMvc.perform(get("/api/ideas").cookie(stranger))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(0));

		// Detail is owner-scoped: 404 for the stranger.
		mockMvc.perform(get("/api/ideas/" + privateIdeaId).cookie(stranger))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/ideas/" + privateIdeaId)
				.cookie(stranger)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"isFavorite\":true}")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	void detailIncludesContentValidationAndStaleFlag() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId, fieldId(), "Dental reminders", 8, false, IdeaStatus.GENERATED, false);

		mockMvc.perform(get("/api/ideas/" + idea.getId()).cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.title").value("Dental reminders"))
				.andExpect(jsonPath("$.content.solution.keyFeatures.length()").value(3))
				.andExpect(jsonPath("$.validation.scores.demand").value(8))
				.andExpect(jsonPath("$.validation.stale").value(false))
				.andExpect(jsonPath("$.versionNumber").value(1));

		// A new version makes the validation stale.
		IdeaVersion second = versions.save(new IdeaVersion(
				idea.getId(), 2, VersionSource.CHAT, TestIdeas.idea("Dental reminders v2"), "chat update"));
		idea.applyContent("Dental reminders v2", "tagline", second.getId());
		ideas.save(idea);

		mockMvc.perform(get("/api/ideas/" + idea.getId()).cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.versionNumber").value(2))
				.andExpect(jsonPath("$.validation.stale").value(true))
				.andExpect(jsonPath("$.validation.validatedVersionNumber").value(1));
	}

	@Test
	void patchTogglesFavoriteAndArchive() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId, fieldId(), "Toggle me", 6, false, IdeaStatus.GENERATED, false);

		mockMvc.perform(patch("/api/ideas/" + idea.getId())
				.cookie(cookie)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"isFavorite\":true,\"isArchived\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.favorite").value(true))
				.andExpect(jsonPath("$.archived").value(true));
	}

	@Test
	void deleteIsSoft() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId, fieldId(), "Delete me", 5, false, IdeaStatus.GENERATED, false);

		mockMvc.perform(delete("/api/ideas/" + idea.getId()).cookie(cookie)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/ideas/" + idea.getId()).cookie(cookie))
				.andExpect(status().isNotFound());
		assertThat(ideas.findById(idea.getId()).orElseThrow().getDeletedAt()).isNotNull();
	}

	@Test
	void restoreCreatesANewVersion() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId, fieldId(), "Restorable", 6, false, IdeaStatus.IN_PROGRESS, false);
		IdeaVersion second = versions.save(new IdeaVersion(
				idea.getId(), 2, VersionSource.CHAT, TestIdeas.idea("Changed title"), "chat update"));
		idea.applyContent("Changed title", "tagline", second.getId());
		ideas.save(idea);

		mockMvc.perform(post("/api/ideas/" + idea.getId() + "/versions/1/restore")
				.cookie(cookie)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.versionNumber").value(3))
				.andExpect(jsonPath("$.source").value("RESTORE"))
				.andExpect(jsonPath("$.content.title").value("Restorable"));

		Idea restored = ideas.findById(idea.getId()).orElseThrow();
		assertThat(restored.getTitle()).isEqualTo("Restorable");
		assertThat(versions.findById(restored.getCurrentVersionId()).orElseThrow().getVersionNumber()).isEqualTo(3);

		mockMvc.perform(get("/api/ideas/" + idea.getId() + "/versions").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].versionNumber").value(3));
	}

	@Test
	void finalizedIdeasAreLocked() throws Exception {
		Cookie cookie = login(uniqueEmail());
		UUID userId = currentUserId();
		Idea idea = seedIdea(userId, fieldId(), "Locked idea", 8, false, IdeaStatus.FINALIZED, false);

		mockMvc.perform(post("/api/ideas/" + idea.getId() + "/versions/1/restore")
				.cookie(cookie)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IDEA_LOCKED"));
	}

	// ---- helpers ----

	private String uniqueLastEmail;

	private String uniqueEmail() {
		uniqueLastEmail = "ideas-" + UUID.randomUUID() + "@example.com";
		return uniqueLastEmail;
	}

	private UUID currentUserId() {
		User user = users.findByEmailNormalized(emailNormalizer.normalize(uniqueLastEmail)).orElseThrow();
		return user.getId();
	}

	private UUID fieldId() {
		return fields.findByActiveTrueAndParentIdIsNullOrderBySortOrderAsc().get(0).getId();
	}

	private Idea seedIdea(
			UUID userId,
			UUID fieldId,
			String title,
			int demand,
			boolean favorite,
			IdeaStatus status,
			boolean archived) {
		Idea idea = new Idea(userId, null, fieldId, null, title, "tagline for " + title);
		ideas.save(idea);
		IdeaVersion version = versions.save(new IdeaVersion(
				idea.getId(), 1, VersionSource.GENERATION, TestIdeas.idea(title), "Initial generation"));
		idea.applyContent(title, "tagline for " + title, version.getId());
		validations.save(new IdeaValidation(
				idea.getId(), version.getId(), ValidationDepth.LIGHT,
				demand, 7, 4, 6, true,
				new ScoreRationales("d", "p", "c", "n"),
				List.of(), List.of(), List.of("query")));
		if (favorite) {
			idea.setFavorite(true);
		}
		if (archived) {
			idea.setArchived(true);
		}
		if (status != IdeaStatus.GENERATED) {
			idea.setStatus(status);
		}
		return ideas.save(idea);
	}
}
