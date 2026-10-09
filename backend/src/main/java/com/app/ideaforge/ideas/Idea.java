package com.app.ideaforge.ideas;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ideas")
public class Idea {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "generation_run_id")
	private UUID generationRunId;

	@Column(name = "field_id", nullable = false)
	private UUID fieldId;

	private String niche;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IdeaStatus status = IdeaStatus.GENERATED;

	@Column(name = "is_favorite", nullable = false)
	private boolean favorite;

	@Column(name = "is_archived", nullable = false)
	private boolean archived;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String tagline;

	@Column(name = "current_version_id")
	private UUID currentVersionId;

	@Column(name = "message_count", nullable = false)
	private int messageCount;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	@Column(name = "finalized_at")
	private Instant finalizedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected Idea() {
	}

	public Idea(UUID userId, UUID generationRunId, UUID fieldId, String niche, String title, String tagline) {
		this.userId = userId;
		this.generationRunId = generationRunId;
		this.fieldId = fieldId;
		this.niche = niche;
		this.title = title;
		this.tagline = tagline;
	}

	public void applyContent(String title, String tagline, UUID versionId) {
		this.title = title;
		this.tagline = tagline;
		this.currentVersionId = versionId;
		this.updatedAt = Instant.now();
	}

	public void setStatus(IdeaStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
		this.updatedAt = Instant.now();
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
		this.updatedAt = Instant.now();
	}

	public void incrementMessageCount() {
		this.messageCount++;
		this.updatedAt = Instant.now();
	}

	public void markFinalized() {
		this.status = IdeaStatus.FINALIZED;
		this.finalizedAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public void softDelete() {
		this.deletedAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getGenerationRunId() {
		return generationRunId;
	}

	public UUID getFieldId() {
		return fieldId;
	}

	public String getNiche() {
		return niche;
	}

	public IdeaStatus getStatus() {
		return status;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public boolean isArchived() {
		return archived;
	}

	public String getTitle() {
		return title;
	}

	public String getTagline() {
		return tagline;
	}

	public UUID getCurrentVersionId() {
		return currentVersionId;
	}

	public int getMessageCount() {
		return messageCount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getFinalizedAt() {
		return finalizedAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}
}
