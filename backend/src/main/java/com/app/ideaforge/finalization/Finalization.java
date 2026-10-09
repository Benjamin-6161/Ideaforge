package com.app.ideaforge.finalization;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.ideaforge.finalization.dto.FinalizePlan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One finalization per idea; output, build prompt, and AGENTS.md. */
@Entity
@Table(name = "finalizations")
public class Finalization {

	public enum Status {
		RUNNING,
		SUCCEEDED,
		FAILED
	}

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "idea_id", nullable = false, unique = true)
	private UUID ideaId;

	@Column(name = "version_id", nullable = false)
	private UUID versionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.RUNNING;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private FinalizePlan output;

	@Column(name = "build_prompt_md")
	private String buildPromptMd;

	@Column(name = "agents_md")
	private String agentsMd;

	@Column(name = "spend_ledger_id")
	private UUID spendLedgerId;

	@Column(name = "refund_ledger_id")
	private UUID refundLedgerId;

	@Column(name = "prompt_version")
	private String promptVersion;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "completed_at")
	private Instant completedAt;

	protected Finalization() {
	}

	public Finalization(UUID ideaId, UUID versionId, UUID spendLedgerId, String promptVersion) {
		this.ideaId = ideaId;
		this.versionId = versionId;
		this.spendLedgerId = spendLedgerId;
		this.promptVersion = promptVersion;
	}

	public void succeed(FinalizePlan plan, String buildPromptMd, String agentsMd) {
		this.output = plan;
		this.buildPromptMd = buildPromptMd;
		this.agentsMd = agentsMd;
		this.status = Status.SUCCEEDED;
		this.completedAt = Instant.now();
	}

	public void fail() {
		this.status = Status.FAILED;
		this.completedAt = Instant.now();
	}

	public void markRefunded(UUID refundLedgerId) {
		this.refundLedgerId = refundLedgerId;
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

	public Status getStatus() {
		return status;
	}

	public FinalizePlan getOutput() {
		return output;
	}

	public String getBuildPromptMd() {
		return buildPromptMd;
	}

	public String getAgentsMd() {
		return agentsMd;
	}

	public UUID getSpendLedgerId() {
		return spendLedgerId;
	}

	public UUID getRefundLedgerId() {
		return refundLedgerId;
	}

	public String getPromptVersion() {
		return promptVersion;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}
}
