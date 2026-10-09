package com.app.ideaforge.ideas;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.ideaforge.ideas.dto.Competitor;
import com.app.ideaforge.ideas.dto.EvidenceItem;
import com.app.ideaforge.ideas.dto.ScoreRationales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "idea_validations")
public class IdeaValidation {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "idea_id", nullable = false)
	private UUID ideaId;

	@Column(name = "version_id", nullable = false)
	private UUID versionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ValidationDepth depth;

	@Min(1) @Max(10)
	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "demand_score", nullable = false)
	private int demandScore;

	@Min(1) @Max(10)
	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "profitability_score", nullable = false)
	private int profitabilityScore;

	@Min(1) @Max(10)
	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "competition_score", nullable = false)
	private int competitionScore;

	@Min(1) @Max(10)
	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "novelty_score", nullable = false)
	private int noveltyScore;

	@Column(name = "evidence_available", nullable = false)
	private boolean evidenceAvailable;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private ScoreRationales rationale = new ScoreRationales(null, null, null, null);

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private List<Competitor> competitors = List.of();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private List<EvidenceItem> evidence = List.of();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private List<String> queries = List.of();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected IdeaValidation() {
	}

	public IdeaValidation(
			UUID ideaId,
			UUID versionId,
			ValidationDepth depth,
			int demandScore,
			int profitabilityScore,
			int competitionScore,
			int noveltyScore,
			boolean evidenceAvailable,
			ScoreRationales rationale,
			List<Competitor> competitors,
			List<EvidenceItem> evidence,
			List<String> queries) {
		this.ideaId = ideaId;
		this.versionId = versionId;
		this.depth = depth;
		this.demandScore = demandScore;
		this.profitabilityScore = profitabilityScore;
		this.competitionScore = competitionScore;
		this.noveltyScore = noveltyScore;
		this.evidenceAvailable = evidenceAvailable;
		this.rationale = rationale;
		this.competitors = competitors == null ? List.of() : competitors;
		this.evidence = evidence == null ? List.of() : evidence;
		this.queries = queries == null ? List.of() : queries;
	}

	public UUID getId() {
		return id;
	}

	public UUID getIdeaId() {
		return ideaId;
	}

	public UUID getVersionId() {
		return versionId;
	}

	public ValidationDepth getDepth() {
		return depth;
	}

	public int getDemandScore() {
		return demandScore;
	}

	public int getProfitabilityScore() {
		return profitabilityScore;
	}

	public int getCompetitionScore() {
		return competitionScore;
	}

	public int getNoveltyScore() {
		return noveltyScore;
	}

	public boolean isEvidenceAvailable() {
		return evidenceAvailable;
	}

	public ScoreRationales getRationale() {
		return rationale;
	}

	public List<Competitor> getCompetitors() {
		return competitors;
	}

	public List<EvidenceItem> getEvidence() {
		return evidence;
	}

	public List<String> getQueries() {
		return queries;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
