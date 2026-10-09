package com.app.ideaforge.user;

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
@Table(name = "user_identities")
public class UserIdentity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IdentityProvider provider;

	@Column(name = "provider_subject", nullable = false)
	private String providerSubject;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected UserIdentity() {
	}

	public UserIdentity(UUID userId, IdentityProvider provider, String providerSubject) {
		this.userId = userId;
		this.provider = provider;
		this.providerSubject = providerSubject;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public IdentityProvider getProvider() {
		return provider;
	}

	public String getProviderSubject() {
		return providerSubject;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
