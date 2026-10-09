package com.app.ideaforge.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "email_normalized", nullable = false)
	private String emailNormalized;

	@Column(name = "code_hash", nullable = false)
	private String codeHash;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "max_attempts", nullable = false)
	private int maxAttempts;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	@Column(name = "ip_hash")
	private String ipHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected OtpChallenge() {
	}

	public OtpChallenge(String emailNormalized, String codeHash, int maxAttempts, Instant expiresAt, String ipHash) {
		this.emailNormalized = emailNormalized;
		this.codeHash = codeHash;
		this.maxAttempts = maxAttempts;
		this.expiresAt = expiresAt;
		this.ipHash = ipHash;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getEmailNormalized() {
		return emailNormalized;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public int getAttempts() {
		return attempts;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}

	public String getIpHash() {
		return ipHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void incrementAttempts() {
		this.attempts++;
	}

	public void consume(Instant when) {
		this.consumedAt = when;
	}

	public boolean isConsumed() {
		return consumedAt != null;
	}
}
