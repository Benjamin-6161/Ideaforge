package com.app.ideaforge.ideas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.common.web.PageResponse;
import com.app.ideaforge.fields.Field;
import com.app.ideaforge.fields.FieldRepository;
import com.app.ideaforge.ideas.dto.IdeaDetail;
import com.app.ideaforge.ideas.dto.IdeaListItem;
import com.app.ideaforge.ideas.dto.PatchIdeaRequest;
import com.app.ideaforge.ideas.dto.RevalidationInfo;
import com.app.ideaforge.ideas.dto.ScoreSummary;
import com.app.ideaforge.ideas.dto.ValidationDetail;
import com.app.ideaforge.ideas.dto.VersionSummary;

/**
 * Idea read/update operations. Everything is scoped to the authenticated user;
 * other users' resources return 404.
 */
@Service
public class IdeaService {

	private static final int MAX_PAGE_SIZE = 50;
	private static final List<String> SORTS = List.of("newest", "demand", "profitability");

	private final IdeaRepository ideas;
	private final IdeaVersionRepository versions;
	private final IdeaValidationRepository validations;
	private final FieldRepository fields;
	private final ValidationRunRepository validationRuns;

	public IdeaService(
			IdeaRepository ideas,
			IdeaVersionRepository versions,
			IdeaValidationRepository validations,
			FieldRepository fields,
			ValidationRunRepository validationRuns) {
		this.ideas = ideas;
		this.versions = versions;
		this.validations = validations;
		this.fields = fields;
		this.validationRuns = validationRuns;
	}

	@Transactional(readOnly = true)
	public PageResponse<IdeaListItem> list(
			UUID userId,
			UUID fieldId,
			String status,
			Boolean favorite,
			boolean archived,
			String q,
			String sort,
			int page,
			int size) {
		String statusFilter = normalizeStatus(status);
		String sortFilter = normalizeSort(sort);
		String query = q == null || q.isBlank() ? null : q.trim();
		Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
		Page<Idea> result = ideas.search(userId, fieldId, statusFilter, favorite, archived, query, sortFilter, pageable);

		List<UUID> ideaIds = result.getContent().stream().map(Idea::getId).toList();
		Map<UUID, IdeaValidation> latest = latestValidations(ideaIds);
		Map<UUID, Field> fieldNames = loadFields(result.getContent());
		Map<UUID, Integer> versionNumbers = loadVersionNumbers(result.getContent());

		return PageResponse.from(result.map(idea -> toListItem(
				idea,
				fieldNames.get(idea.getFieldId()),
				latest.get(idea.getId()),
				versionNumbers.getOrDefault(idea.getCurrentVersionId(), 1))));
	}

	@Transactional(readOnly = true)
	public IdeaDetail detail(UUID userId, UUID ideaId) {
		Idea idea = requireOwned(userId, ideaId);
		IdeaVersion current = idea.getCurrentVersionId() == null
				? null
				: versions.findById(idea.getCurrentVersionId()).orElse(null);
		IdeaValidation validation = validations.findFirstByIdeaIdOrderByCreatedAtDesc(ideaId).orElse(null);
		String fieldName = fields.findById(idea.getFieldId()).map(Field::getName).orElse("");
		int versionNumber = current == null ? 1 : current.getVersionNumber();

		ValidationDetail validationDetail = null;
		if (validation != null) {
			int validatedVersionNumber = versions.findById(validation.getVersionId())
					.map(IdeaVersion::getVersionNumber)
					.orElse(0);
			boolean stale = idea.getCurrentVersionId() == null
					|| !validation.getVersionId().equals(idea.getCurrentVersionId());
			validationDetail = new ValidationDetail(
					toScoreSummary(validation),
					validation.getRationale(),
					validation.getCompetitors(),
					validation.getEvidence(),
					validation.getQueries(),
					validation.getCreatedAt(),
					validatedVersionNumber,
					stale);
		}

		RevalidationInfo revalidation = validationRuns.findFirstByIdeaIdOrderByCreatedAtDesc(ideaId)
				.map(run -> new RevalidationInfo(
						run.getStatus().name(),
						run.getErrorCode(),
						run.getCreatedAt(),
						run.getCompletedAt()))
				.orElse(null);

		return new IdeaDetail(
				idea.getId(),
				idea.getFieldId(),
				fieldName,
				idea.getNiche(),
				idea.getTitle(),
				idea.getTagline(),
				idea.getStatus().name(),
				idea.isFavorite(),
				idea.isArchived(),
				idea.getCurrentVersionId(),
				versionNumber,
				idea.getCreatedAt(),
				idea.getUpdatedAt(),
				idea.getFinalizedAt(),
				current == null ? null : current.getContent(),
				validationDetail,
				revalidation);
	}

	@Transactional
	public IdeaDetail patch(UUID userId, UUID ideaId, PatchIdeaRequest request) {
		Idea idea = requireOwned(userId, ideaId);
		if (request.isFavorite() != null) {
			idea.setFavorite(request.isFavorite());
		}
		if (request.isArchived() != null) {
			idea.setArchived(request.isArchived());
		}
		return detail(userId, ideaId);
	}

	@Transactional
	public void softDelete(UUID userId, UUID ideaId) {
		Idea idea = requireOwned(userId, ideaId);
		idea.softDelete();
	}

	@Transactional(readOnly = true)
	public List<VersionSummary> versions(UUID userId, UUID ideaId) {
		requireOwned(userId, ideaId);
		return versions.findByIdeaIdOrderByVersionNumberDesc(ideaId).stream()
				.map(version -> VersionSummary.from(version, false))
				.toList();
	}

	@Transactional(readOnly = true)
	public VersionSummary version(UUID userId, UUID ideaId, int versionNumber) {
		requireOwned(userId, ideaId);
		IdeaVersion version = versions.findByIdeaIdAndVersionNumber(ideaId, versionNumber)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Version not found."));
		return VersionSummary.from(version, true);
	}

	@Transactional
	public VersionSummary restore(UUID userId, UUID ideaId, int versionNumber) {
		Idea idea = requireOwned(userId, ideaId);
		if (idea.getStatus() == IdeaStatus.FINALIZED) {
			throw ApiException.conflict(ErrorCodes.IDEA_LOCKED,
					"This idea is finalized and can no longer be edited.");
		}
		IdeaVersion source = versions.findByIdeaIdAndVersionNumber(ideaId, versionNumber)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Version not found."));
		int nextNumber = versions.maxVersionNumber(ideaId) + 1;
		IdeaVersion restored = versions.save(new IdeaVersion(
				ideaId,
				nextNumber,
				VersionSource.RESTORE,
				source.getContent(),
				"Restored from version " + versionNumber));
		idea.applyContent(
				source.getContent().title(),
				source.getContent().tagline(),
				restored.getId());
		if (idea.getStatus() == IdeaStatus.GENERATED) {
			idea.setStatus(IdeaStatus.IN_PROGRESS);
		}
		return VersionSummary.from(restored, true);
	}

	private Idea requireOwned(UUID userId, UUID ideaId) {
		return ideas.findByIdAndUserIdAndDeletedAtIsNull(ideaId, userId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Idea not found."));
	}

	private Map<UUID, IdeaValidation> latestValidations(List<UUID> ideaIds) {
		Map<UUID, IdeaValidation> latest = new HashMap<>();
		if (ideaIds.isEmpty()) {
			return latest;
		}
		for (IdeaValidation validation : validations.findByIdeaIdInOrderByCreatedAtDesc(ideaIds)) {
			latest.putIfAbsent(validation.getIdeaId(), validation);
		}
		return latest;
	}

	private Map<UUID, Field> loadFields(List<Idea> page) {
		Map<UUID, Field> byId = new HashMap<>();
		for (Idea idea : page) {
			byId.computeIfAbsent(idea.getFieldId(), id -> fields.findById(id).orElse(null));
		}
		return byId;
	}

	private Map<UUID, Integer> loadVersionNumbers(List<Idea> page) {
		Map<UUID, Integer> byVersionId = new HashMap<>();
		List<UUID> versionIds = page.stream()
				.map(Idea::getCurrentVersionId)
				.filter(java.util.Objects::nonNull)
				.toList();
		for (IdeaVersion version : versions.findAllById(versionIds)) {
			byVersionId.put(version.getId(), version.getVersionNumber());
		}
		return byVersionId;
	}

	private IdeaListItem toListItem(Idea idea, Field field, IdeaValidation validation, int versionNumber) {
		return new IdeaListItem(
				idea.getId(),
				idea.getFieldId(),
				field == null ? "" : field.getName(),
				idea.getNiche(),
				idea.getTitle(),
				idea.getTagline(),
				idea.getStatus().name(),
				idea.isFavorite(),
				idea.isArchived(),
				idea.getCurrentVersionId(),
				versionNumber,
				idea.getCreatedAt(),
				idea.getUpdatedAt(),
				validation == null ? null : toScoreSummary(validation));
	}

	private ScoreSummary toScoreSummary(IdeaValidation validation) {
		return new ScoreSummary(
				validation.getDemandScore(),
				validation.getProfitabilityScore(),
				validation.getCompetitionScore(),
				validation.getNoveltyScore(),
				validation.isEvidenceAvailable(),
				validation.getDepth().name(),
				validation.getVersionId());
	}

	private String normalizeStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return IdeaStatus.valueOf(status.toUpperCase()).name();
		} catch (IllegalArgumentException ex) {
			throw ApiException.badRequest(ErrorCodes.VALIDATION_ERROR, "Unknown status filter.");
		}
	}

	private String normalizeSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return "newest";
		}
		String normalized = sort.toLowerCase();
		if (!SORTS.contains(normalized)) {
			throw ApiException.badRequest(ErrorCodes.VALIDATION_ERROR, "Unknown sort option.");
		}
		return normalized;
	}

	/** Used by the chat/finalize phases to enforce the locked state. */
	public void assertEditable(Idea idea) {
		if (idea.getStatus() == IdeaStatus.FINALIZED) {
			throw ApiException.conflict(ErrorCodes.IDEA_LOCKED, "This idea is finalized.");
		}
	}
}
