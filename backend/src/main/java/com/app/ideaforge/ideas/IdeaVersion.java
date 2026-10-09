package com.app.ideaforge.ideas;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.ideaforge.ideas.dto.IdeaContent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Immutable content version. Rows are never updated or deleted. */
@Entity
@Table(name = "idea_versions")
public class IdeaVersion {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "idea_id", nullable = false)
	private UUID ideaId;

	@Column(name = "version_number", nullable = false)
	private int versionNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private VersionSource source;

	@Column(name = "schema_version", nullable = false)
	private int schemaVersion = 1;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private IdeaContent content;

	@Column(name = "change_summary")
	private String changeSummary;

	@Column(name = "created_from_message_id")
	private UUID createdFromMessageId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected IdeaVersion() {
	}

	public IdeaVersion(
			UUID ideaId,
			int versionNumber,
			VersionSource source,
			IdeaContent content,
			String changeSummary) {
		this(ideaId, versionNumber, source, content, changeSummary, null);
	}

	public IdeaVersion(
			UUID ideaId,
			int versionNumber,
			VersionSource source,
			IdeaContent content,
			String changeSummary,
			UUID createdFromMessageId) {
		this.ideaId = ideaId;
		this.versionNumber = versionNumber;
		this.source = source;
		this.content = content;
		this.changeSummary = changeSummary;
		this.createdFromMessageId = createdFromMessageId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getIdeaId() {
		return ideaId;
	}

	public int getVersionNumber() {
		return versionNumber;
	}

	public VersionSource getSource() {
		return source;
	}

	public int getSchemaVersion() {
		return schemaVersion;
	}

	public IdeaContent getContent() {
		return content;
	}

	public String getChangeSummary() {
		return changeSummary;
	}

	public UUID getCreatedFromMessageId() {
		return createdFromMessageId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
