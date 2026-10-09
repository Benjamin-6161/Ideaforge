package com.app.ideaforge.generation.domain;

public enum CustomerSize implements LabeledOption {
	SOLO("Solo"),
	SMALL_BUSINESS("Small business (1-50)"),
	MID_MARKET("Mid-market"),
	ENTERPRISE("Enterprise"),
	ANY("Any");

	private final String label;

	CustomerSize(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
