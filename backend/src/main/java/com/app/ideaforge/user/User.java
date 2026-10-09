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
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false)
	private String email;

	@Column(name = "email_normalized", nullable = false, unique = true)
	private String emailNormalized;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "avatar_url")
	private String avatarUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role = UserRole.USER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "signup_ip_hash")
	private String signupIpHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	protected User() {
	}

	public User(String email, String emailNormalized, String signupIpHash) {
		this.email = email;
		this.emailNormalized = emailNormalized;
		this.signupIpHash = signupIpHash;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getEmailNormalized() {
		return emailNormalized;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public UserRole getRole() {
		return role;
	}

	public void setRole(UserRole role) {
		this.role = role;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

	public Instant getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	public void setEmailVerifiedAt(Instant emailVerifiedAt) {
		this.emailVerifiedAt = emailVerifiedAt;
	}

	public String getSignupIpHash() {
		return signupIpHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Instant lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}
}
