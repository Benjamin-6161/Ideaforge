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

/** A deep validation (re-validate) run; tracked for polling and refunds. */
@Entity
@Table(name = "validation_runs")
public class ValidationRun {

	public enum Status {
		RUNNING,
		SUCCEEDED,
		FAILED
	}

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "idea_id", nullable = false)
	private UUID ideaId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "version_id", nullable = false)
	private UUID versionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.RUNNING;

	@Column(name = "spend_ledger_id")
	private UUID spendLedgerId;

	@Column(name = "refund_ledger_id")
	private UUID refundLedgerId;

	@Column(name = "error_code")
	private String errorCode;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "completed_at")
	private Instant completedAt;

	protected ValidationRun() {
	}

	public ValidationRun(UUID ideaId, UUID userId, UUID versionId, UUID spendLedgerId) {
		this.ideaId = ideaId;
		this.userId = userId;
		this.versionId = versionId;
		this.spendLedgerId = spendLedgerId;
	}

	public void succeed() {
		this.status = Status.SUCCEEDED;
		this.completedAt = Instant.now();
	}

	public void fail(String errorCode) {
		this.status = Status.FAILED;
		this.errorCode = errorCode;
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

	public UUID getUserId() {
		return userId;
	}

	public UUID getVersionId() {
		return versionId;
	}

	public Status getStatus() {
		return status;
	}

	public UUID getSpendLedgerId() {
		return spendLedgerId;
	}

	public UUID getRefundLedgerId() {
		return refundLedgerId;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}
}
