package com.app.ideaforge.credits;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_wallets")
public class CreditWallet {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "balance_units", nullable = false)
	private long balanceUnits;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected CreditWallet() {
	}

	public CreditWallet(UUID userId, long balanceUnits) {
		this.userId = userId;
		this.balanceUnits = balanceUnits;
	}

	public UUID getUserId() {
		return userId;
	}

	public long getBalanceUnits() {
		return balanceUnits;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
