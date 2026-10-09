package com.app.ideaforge.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@Column(name = "ip_hash")
	private String ipHash;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt = Instant.now();

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected AuthSession() {
	}

	public AuthSession(UUID userId, String tokenHash, String ipHash, String userAgent, Instant expiresAt) {
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.ipHash = ipHash;
		this.userAgent = userAgent;
		this.createdAt = Instant.now();
		this.lastSeenAt = this.createdAt;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public String getIpHash() {
		return ipHash;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public void touch(Instant seenAt, Instant newExpiry) {
		this.lastSeenAt = seenAt;
		this.expiresAt = newExpiry;
	}

	public void revoke(Instant when) {
		this.revokedAt = when;
	}

	public boolean isValid(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}
}
