package com.app.ideaforge.llm;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row per LLM call; never stores prompts or responses. */
@Entity
@Table(name = "ai_usage_logs")
public class AiUsageLog {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id")
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LlmPurpose purpose;

	@Column(nullable = false)
	private String provider;

	@Column(nullable = false)
	private String model;

	@Column(name = "tokens_in", nullable = false)
	private int tokensIn;

	@Column(name = "tokens_out", nullable = false)
	private int tokensOut;

	@Column(name = "latency_ms", nullable = false)
	private int latencyMs;

	@Column(name = "est_cost_micro_usd", nullable = false)
	private long estCostMicroUsd;

	@Column(nullable = false)
	private boolean success;

	@Column(name = "ref_type")
	private String refType;

	@Column(name = "ref_id")
	private UUID refId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected AiUsageLog() {
	}

	public AiUsageLog(
			UUID userId,
			LlmPurpose purpose,
			String provider,
			String model,
			int tokensIn,
			int tokensOut,
			int latencyMs,
			long estCostMicroUsd,
			boolean success,
			String refType,
			UUID refId) {
		this.userId = userId;
		this.purpose = purpose;
		this.provider = provider;
		this.model = model;
		this.tokensIn = tokensIn;
		this.tokensOut = tokensOut;
		this.latencyMs = latencyMs;
		this.estCostMicroUsd = estCostMicroUsd;
		this.success = success;
		this.refType = refType;
		this.refId = refId;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public LlmPurpose getPurpose() {
		return purpose;
	}

	public String getProvider() {
		return provider;
	}

	public String getModel() {
		return model;
	}

	public int getTokensIn() {
		return tokensIn;
	}

	public int getTokensOut() {
		return tokensOut;
	}

	public int getLatencyMs() {
		return latencyMs;
	}

	public long getEstCostMicroUsd() {
		return estCostMicroUsd;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getRefType() {
		return refType;
	}

	public UUID getRefId() {
		return refId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
