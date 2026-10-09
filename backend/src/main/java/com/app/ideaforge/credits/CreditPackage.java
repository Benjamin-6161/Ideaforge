package com.app.ideaforge.credits;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_packages")
public class CreditPackage {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(nullable = false)
	private String name;

	@Column(name = "credits_units", nullable = false)
	private int creditsUnits;

	@Column(name = "price_cents", nullable = false)
	private int priceCents;

	@Column(nullable = false)
	private String currency;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected CreditPackage() {
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public int getCreditsUnits() {
		return creditsUnits;
	}

	public int getPriceCents() {
		return priceCents;
	}

	public String getCurrency() {
		return currency;
	}

	public boolean isActive() {
		return active;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
