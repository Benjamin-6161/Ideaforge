package com.app.ideaforge.generation.domain;

public enum StartingBudget implements LabeledOption {
	MINIMAL("Minimal"),
	MODERATE("Moderate"),
	FLEXIBLE("Flexible");

	private final String label;

	StartingBudget(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
