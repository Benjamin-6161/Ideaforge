package com.app.ideaforge.generation;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "generation_runs")
public class GenerationRun {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "field_id", nullable = false)
	private UUID fieldId;

	private String niche;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private Map<String, Object> parameters = Map.of();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RunStatus status = RunStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(name = "progress_stage")
	private ProgressStage progressStage;

	@Column(name = "error_code")
	private String errorCode;

	@Column(name = "spend_ledger_id")
	private UUID spendLedgerId;

	@Column(name = "refund_ledger_id")
	private UUID refundLedgerId;

	@Column(name = "prompt_version")
	private String promptVersion;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected GenerationRun() {
	}

	public GenerationRun(UUID userId, UUID fieldId, String niche, Map<String, Object> parameters, UUID spendLedgerId) {
		this.userId = userId;
		this.fieldId = fieldId;
		this.niche = niche;
		this.parameters = parameters;
		this.spendLedgerId = spendLedgerId;
	}

	public void start(String promptVersion) {
		this.status = RunStatus.RUNNING;
		this.progressStage = ProgressStage.IDEATING;
		this.startedAt = Instant.now();
		this.promptVersion = promptVersion;
	}

	public void stage(ProgressStage stage) {
		this.progressStage = stage;
	}

	public void succeed() {
		this.status = RunStatus.SUCCEEDED;
		this.progressStage = ProgressStage.DONE;
		this.completedAt = Instant.now();
	}

	public void fail(String errorCode) {
		this.status = RunStatus.FAILED;
		this.errorCode = errorCode;
		this.completedAt = Instant.now();
	}

	public void markRefunded(UUID refundLedgerId) {
		this.refundLedgerId = refundLedgerId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getFieldId() {
		return fieldId;
	}

	public String getNiche() {
		return niche;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public RunStatus getStatus() {
		return status;
	}

	public ProgressStage getProgressStage() {
		return progressStage;
	}

	public String getErrorCode() {
		return errorCode;
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

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}
}
