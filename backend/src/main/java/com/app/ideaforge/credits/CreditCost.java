package com.app.ideaforge.credits;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_costs")
public class CreditCost {

	@Id
	private String action;

	@Column(name = "cost_units", nullable = false)
	private int costUnits;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	protected CreditCost() {
	}

	public String getAction() {
		return action;
	}

	public int getCostUnits() {
		return costUnits;
	}

	public boolean isActive() {
		return active;
	}
}
