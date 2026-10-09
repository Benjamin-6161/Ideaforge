package com.app.ideaforge.security;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false)
	private String type;

	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "ip_hash")
	private String ipHash;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private Map<String, Object> details = Map.of();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected SecurityEvent() {
	}

	public SecurityEvent(String type, UUID userId, String ipHash, Map<String, Object> details) {
		this.type = type;
		this.userId = userId;
		this.ipHash = ipHash;
		this.details = details == null ? Map.of() : details;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getIpHash() {
		return ipHash;
	}

	public Map<String, Object> getDetails() {
		return details;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
